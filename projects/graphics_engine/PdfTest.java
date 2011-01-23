
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;

/**
 * Test of the PDF-writing library.
 */
public class PdfTest {
    public static void main(String[] args) throws Exception {
        // int width = 5220;
        // int height = 3375;
        String imageFilename = "cover.png";
        int width = 1253;
        int height = 810;
        // String imageFilename = "cover_small.png";
        Document document = new Document(new Rectangle(0, 0, width, height), 0, 0, 0, 0);
        PdfWriter.getInstance(document, new FileOutputStream("ge_cover.pdf"));
        document.open();
        Image image = Image.getInstance(imageFilename);
        image.scaleAbsolute(width, height);
        // Setting DPI didn't do anything:
        /// image.setDpi(300, 300);
        /// System.out.println(image.getDpiX());
        /// System.out.println(image.getDpiY());
        document.add(image);
        document.close();
    }
}
