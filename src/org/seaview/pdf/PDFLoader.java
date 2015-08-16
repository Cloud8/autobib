package org.seaview.pdf;

import org.seaview.data.AbstractAnalyzer;
import org.shanghai.util.FileUtil;
import org.seaview.util.TextUtil;
import org.seaview.util.HyphenRemover;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.DCTerms;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdfwriter.COSWriter;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.text.PDFTextStripper;

import net.ricecode.similarity.JaroWinklerStrategy;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityService;
import net.ricecode.similarity.StringSimilarityServiceImpl;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.net.URL;
import java.net.MalformedURLException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.List;
import java.util.logging.Logger;

public class PDFLoader extends AbstractAnalyzer {

    private PDDocument document;
    private static final String scratchFile = 
                         System.getProperty("java.io.tmpdir") + "/seaview.pdf";
    public int size;
    private SimpleDateFormat sdf; 

    public PDFLoader() {
    }

    public PDFLoader(String docbase) {
        super.docbase = docbase;
    }

    public PDFLoader create() {
        size = 0;
        document = null;
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        return this;
    }

    public void dispose() {
        if (document==null) {
            return ;
        }
        Path path = Paths.get(scratchFile);
        try { 
		    document.close(); 
            if (Files.exists(path)) {
                Files.delete(path);
            }
		} catch (IOException e) { log(e); }
        document = null;
    }

    @Override
    public void analyze(Model model, Resource rc, String id) {
        try {
            Path check = Paths.get(id);
            if ( Files.isRegularFile(check) && id.endsWith(".pdf") ) {
                log("load file " + id);
                document = PDDocument.load(Files.newInputStream(check));
            } else {
                String path = getPath(model, rc, id, ".pdf");
                if (path==null) {
                    return;
                }
                check = Paths.get(path);
                if ( Files.isRegularFile(check) && path.endsWith(".pdf") ) {
                    log("load path " + path);
                    document = PDDocument.load(Files.newInputStream(check));
                } else {
                    log("load url " + path);
                    URL curl = new URL(path);
                    document = PDDocument.load(curl.openStream());
                }
            }
            size = document.getNumberOfPages();
        } catch (IOException e) { log(e); }
    }

    @Override
    public Resource test(Model model, String id) {
		log("test " + id);
		return analyze(model, id);
    }

    public void dump(Model model, String id, String fname) {
        Resource rc = findResource(model, id);
        analyze(model, rc, id);
        save(model, rc, fname);
    }

    public boolean failed() {
        if (document==null) {
            return true;
        }
        return false;
    }

    public String maltreat(int start, double tail) {
        try {
            size = document.getNumberOfPages();
            if (tail < 1.0 && size>33) {
                int count = (int) (tail * size);
                //log("removing " + (size-start) + " pages");
                for (int i=start; i<count; i++) {
                    document.removePage(start);
                }
            }
            if (scratchFile!=null) {
                write(scratchFile, document);
            }
        } //catch (FileNotFoundException e) { log(e); }
          //catch (IOException e) { log(e); }
          catch (IllegalArgumentException e) { log(e); }
        return scratchFile;
    }

    public InputStream createInputStream() {
        InputStream is = null;
        try {
            if (scratchFile!=null) {
                is = new FileInputStream(scratchFile);
            } else {
                is = new PDStream(document).createInputStream();
            }
        } catch (IOException e) { log(e); }
        finally {
            return is;
        }
    }
  
    public String fulltext(Model model, Resource rc, String id) {
        int start = 0;
        String path = getPath(model, rc, id, ".pdf");
        int x = path.lastIndexOf(".");
		path = x>0?path.substring(0, x) + ".txt" : path;

        if (x>0 && Files.isRegularFile(Paths.get(path))) {
            //log("reading " + path);
            return FileUtil.read(path);
        }

        String text = null;
        if (document==null) {
            analyze(model, rc, id);
        }
        int end = document.getNumberOfPages();
        log("extract " + end + " pages " + id);
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            //stripper.setForceParsing( true );
            //stripper.setSortByPosition( true );
            stripper.setShouldSeparateByBeads( true );
            stripper.setStartPage(start);
            stripper.setEndPage(end);
            stripper.setAddMoreFormatting(true);
            StringWriter writer = new StringWriter();
            stripper.writeText(document, writer);
            text = writer.toString();
            text = TextUtil.cleanUTF(text);    
            text = HyphenRemover.dehyphenate(text, id);
            text = TextUtil.sentence(text);
            log("write " + path);
            FileUtil.write(path, text);
        } catch (IOException e) {
            log(e);
            e.printStackTrace();
        } finally { 
            return text;
        }
    }

    public PDPage getPageOne() {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDPageTree root = catalog.getPages();
        if (root.getCount() == 0) {
            log("no pages found.");
            return null;
        }
        return root.get(0);
    }

    private void write(String filename, PDDocument document) {
        FileOutputStream output = null;
        COSWriter writer = null;
        try
        {
            output = new FileOutputStream(filename);
            writer = new COSWriter(output);
            writer.write(document);
        } catch ( IOException e ) { log(e); }
        finally
        {
            if( output != null )
            {
                try {
                    output.close();
                    output = null;
                } catch (IOException e) { log(e); }
            }
            if( writer != null )
            {
                try {
                    writer.close();
                    writer = null;
                } catch (IOException e) { log(e); }
            }
        }
    }

    private void decrypt() {
        if (document.isEncrypted()) {
            log("document.isEncrypted");
            //DecryptionMaterial dm = new StandardDecryptionMaterial("");
            //document.openProtection(dm); //GH201502 : gone ?
            document.setAllSecurityToBeRemoved(true);
        }
    }

    /** write resource description to document catalog */
    private void save(Model model, Resource rc, String outfile) {
        boolean b=false;
        decrypt();
        PDDocumentInformation info = document.getDocumentInformation();

        String ctitle = info.getTitle();
        String dtitle = rc.getProperty(DCTerms.title).getString();

        SimilarityStrategy st = new JaroWinklerStrategy();
        StringSimilarityService service = new StringSimilarityServiceImpl(st);
        double score = 0.0;
        if (ctitle!=null) {
            score = service.score(ctitle, dtitle);
        }
        if (score<5.0) {
            info.setTitle(dtitle);
            b=true;
        }

        String cauthor = info.getAuthor();
        if (cauthor == null ) {
            cauthor = info.getCreator();
        }
        Property name = model.createProperty(foaf, "name");
        String dauthor = rc.getProperty(DCTerms.creator)
                           .getResource().getProperty(name).getString();
        score = 0.0;
        if (cauthor!=null) {
            score = service.score(cauthor, dauthor);
        }
        if (score<5.0) {
            info.setAuthor(dauthor);
            b=true;
        }


        if (rc.hasProperty(DCTerms.subject)) {
		    String subjects = null;
			StmtIterator si = rc.listProperties(DCTerms.subject);
            while (si.hasNext()) {
			    Statement stmt = si.next();
				if (stmt.getObject().isLiteral()) {
				    if (subjects==null) subjects = stmt.getString();
                    else subjects += " " + stmt.getString();
				    //log( "[" + stmt.getString() + "]");
				}
            }
			if (subjects!=null) {
                info.setSubject(subjects);
			}
        }

        if (rc.hasProperty(DCTerms.issued)) {
            String iss = rc.getProperty(DCTerms.issued).getString();
            Calendar date = Calendar.getInstance();
            try {
                date.setTime(sdf.parse(iss));
                info.setCreationDate(date);
            } catch (ParseException e) { log(e); }
        }

        info.setProducer("Seaview 1.1");
        info.setCreator((String)null);

        if (b) try {
            int size = document.getDocumentCatalog().getPages().getCount();
            //log("ctitle " + ctitle + " cauthor " + cauthor + " size " + size);
            log(" size " + size);
            document.save(outfile);
        } catch (IOException e) { log(e); }
    }

    public String getTitle() {
        return document.getDocumentInformation().getTitle();
    }

    public String getAuthor() {
        return document.getDocumentInformation().getAuthor();
    }

    public String getDate() {
        Calendar date = document.getDocumentInformation().getCreationDate();
        if (date==null) {
            return null;
        }
        return sdf.format(date.getTime());
    }

    protected static Logger logger =
                         Logger.getLogger(PDFLoader.class.getName());

    protected void log(String msg) {
        logger.info(msg);
    }

    protected void log(Exception e) {
        logger.info(e.toString());
    }

}
