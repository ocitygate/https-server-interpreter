import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;


public class XImage
{
	public static String Resize(String image_data, String resize, Long width, Long height, String background, String format, Long quality)
	{
		if ("NONE".equals(resize))
		{
			return image_data;
		}
		else
		{
			byte[] data = null;
			try { data = image_data.getBytes("ISO-8859-1");	}
			catch (UnsupportedEncodingException e) { }
			BufferedImage image_orig = null;
			try { image_orig = ImageIO.read(new ByteArrayInputStream(data)); } 
			catch (IOException e) { }
			
			BufferedImage image_new;

			int sx = image_orig.getWidth();
			int sy = image_orig.getHeight();

			int dx = width.intValue();
			int dy = height.intValue();

			if ("COMPRESS".equals(resize))
			{
				dx = sx;
				dy = sy;
			}
			else
			{
				dx = width.intValue();
				dy = height.intValue();
			}			

			if ("image/png".equals(format))
				image_new = new BufferedImage(dx, dy, BufferedImage.TYPE_INT_ARGB);
			else
				image_new = new BufferedImage(dx, dy, BufferedImage.TYPE_INT_RGB);
			
			Graphics2D g2d = image_new.createGraphics();

			g2d.setComposite(AlphaComposite.Clear);
			g2d.fillRect(0, 0, dx, dy);

			if ("".equals(background)) background = "image/png".equals(format) ? "00ffffff" : "ffffff";

			g2d.setComposite(AlphaComposite.SrcOver);
			g2d.setColor(ColorDecode(background));
			g2d.fillRect(0, 0, dx, dy);

			int bwidth = 0;
			int bheight = 0;

			switch (resize)
			{
				case "COMPRESS":
				case "STRECTH":
					bwidth = dx;
					bheight = dy;
					
				case "LETTERBOX":
				case "CROP":
					float x_scale = (float)dx / (float)sx;
					float y_scale = (float)dy / (float)sy;
					float scale = "CROP".equals(resize) ? Math.max(x_scale, y_scale) : Math.min(x_scale, y_scale);

					bwidth = (int)(scale * sx);
					bheight = (int)(scale * sy);
					break;
			}

			g2d.drawImage(
					image_orig.getScaledInstance(bwidth, bheight, Image.SCALE_SMOOTH),
					(dx - bwidth) / 2,
					(dy - bheight) / 2,
					bwidth,
					bheight, null);

			ByteArrayOutputStream out = new ByteArrayOutputStream();

			try
			{
				switch(format)
				{
					case "image/jpeg":
						
						MemoryCacheImageOutputStream outputStream = new MemoryCacheImageOutputStream(out);
						
						Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
				        ImageWriter writer = iter.next();
				        ImageWriteParam iwp = writer.getDefaultWriteParam();
				        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				        iwp.setCompressionQuality(quality == null ? 0.7F : (float)quality / 100F);
				        writer.setOutput(outputStream);
				        IIOImage image = new IIOImage(image_new, null, null);
				        writer.write(null, image, iwp);
						break;
					case "image/png":
						ImageIO.write(image_new, "png", out);
						break;
				}
			}
			catch (IOException e) {	}

			try	{ return new String(out.toByteArray(), "ISO-8859-1"); }
			catch (UnsupportedEncodingException e) { }
			
			return null;
		}
	}

	public static Color ColorDecode(String hex)
	{
		int a, r, g, b;
		
		if (hex.length() == 8)
		{
			a = Integer.parseInt(hex.substring(0, 2), 16);
			r = Integer.parseInt(hex.substring(2, 4), 16);
			g = Integer.parseInt(hex.substring(4, 6), 16);
			b = Integer.parseInt(hex.substring(6, 8), 16);
		}
		else if (hex.length() == 6)
		{
			a = 255;
			r = Integer.parseInt(hex.substring(0, 2), 16);
			g = Integer.parseInt(hex.substring(2, 4), 16);
			b = Integer.parseInt(hex.substring(4, 6), 16);
		}
		else
		{
			a = 0;
			r = 255;
			g = 255;
			b = 255;
		}
		
		return new Color(r, g, b, a);
	}
}
