package org.shanghai.rdf;

import org.shanghai.util.FileUtil;

import java.util.Iterator;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Properties;
import java.util.Date;
import java.text.SimpleDateFormat;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.io.StringReader;
import java.io.StringWriter;

/**
   @license http://www.apache.org/licenses/LICENSE-2.0
   @author Goetz Hatop <fb.com/goetz.hatop>
   @title A RDF Transporter with CRUD functionality
   @date 2013-01-17
*/
public class RDFTransporter {

    public interface Reader {
        public void create();
        public void dispose();
        public String[] getSubjects(String q, int offset, int limit);
        public String getDescription(String query, String subject);
        public String query(String query);
    }

    public Properties prop;

    private static final Logger logger =
                         Logger.getLogger(RDFTransporter.class.getName());

    private Reader rdfReader;

    private String probeQuery;
    private String indexQuery;
    private String descrQuery;

    private int size;

    public RDFTransporter(RDFReader.Interface modelTalk) {
        this.rdfReader = new RDFReader(modelTalk);
    }

    public RDFTransporter(Properties prop) {
        this.prop = prop;
        String s = prop.getProperty("index.sparql");
        s=s==null?s=prop.getProperty("store.tdb"):s;
        if (s.equals("http://localhost/terms/store"))
            s=prop.getProperty("store.tdb");
        RDFReader.Interface modelTalk =
                         new ModelTalk( s, prop.getProperty("store.graph"));
        this.rdfReader = new RDFReader(modelTalk);
    }

    private void log(String msg) {
        logger.info(msg);    
    }

    private void log(Exception e) {
        log(e.toString());
        e.printStackTrace(System.out);
    }

    /** create queries, resolve <date> */
    public void create() {
        String date = prop.getProperty("index.date");
        probeQuery = FileUtil.read(prop.getProperty("index.probe"));
        indexQuery = FileUtil.read(prop.getProperty("index.enum"));
        if (indexQuery==null || indexQuery.trim().length()==0) 
            log("everything is wrong.");
        if (date!=null) {
            probeQuery = probeQuery.replace("<date>", date);
            indexQuery = indexQuery.replace("<date>", date);
        } else {
            probeQuery = probeQuery.replace("<date>", "1970-01-01");
            indexQuery = indexQuery.replace("<date>", "1970-01-01");
        }
        descrQuery = FileUtil.read(prop.getProperty("index.dump"));
        rdfReader.create();
        size=0;
    }

    public void dispose() {
        if (rdfReader!=null) 
            rdfReader.dispose();
    }

    public String[] getIdentifiers(int offset, int limit) {
        return rdfReader.getSubjects(indexQuery, offset, limit);
    }

    /** return a transformed record as xml String */
    public String getDescription(String subject) {
        return rdfReader.getDescription(descrQuery, subject);
    }

    /** May be later: nice to have the number of triples in the store */
    /*** 
    public int size() {
        if (size>0)
            return size;
        if (probeQuery==null)
            return 2000;
        String result = rdfReader.query(probeQuery);
        try { 
          if (result==null) {
              log("problem with probeQuery [" + probeQuery + "]");
              prop.list(System.out);
              return 0;
          }
          size = Integer.parseInt(result);
          if (size==0) {
              log("query: " + probeQuery);
              log("result: [" + result + "]");
          }
        } catch (NumberFormatException e) { log(e); }
        // log("size " + size);
        return size;
    }
    **/

    public void talk(String what) {
        String rdf = rdfReader.getDescription(descrQuery, what);
        String testRdf = prop.getProperty("index.test");
        if (testRdf==null) {
            System.out.println(rdf);
        } else {
            FileUtil.write(testRdf, rdf);
            log("wrote " + testRdf);
        } 
    }

    private String readResource(String res) {
        InputStream is = RDFTransporter.class.getResourceAsStream(res);
        if (is==null) {            
            is = RDFTransporter.class.getResourceAsStream("lib" + res);
            if (is!=null) log("load lib " + res);        }
        if (is==null) {
            is = getClass().getClassLoader().getResourceAsStream(res);
            if (is!=null) log("class load " + res);
        }
        if (is==null) {
            is = getClass().getClassLoader().getResourceAsStream("lib"+res);
            if (is!=null) log("class load lib " + res);
        }
        if (is==null) {
            return null;
        }
        //stupid scanner tricks
        //java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
        //return s.hasNext() ? s.next() : "";
        return FileUtil.read(is);
    }

    private void write(String outfile) {
        String talk = getRecord();
        FileUtil.write(outfile, talk);
    }

    public String getRecord() {
        String[] ids = rdfReader.getSubjects(indexQuery, 0, 1);
        String rdf = rdfReader.getDescription(descrQuery, ids[0]);
        return rdf;
    }

    /** test a random record */
    public void probe() {
        String result = rdfReader.query(probeQuery);
        log(result);
        // int max = size();
        // if (max==0) {
        //     log("No triples in the store. Check size.");
        //     return;
        // }
        // int off = (int)(Math.random() * max);
        // String[] identifiers = rdfReader.getSubjects(indexQuery, off, 1);
        // String what = identifiers[0];
        // log( "resource " + off + ": " + what );
        // talk(what);
    }
}
