package org.shanghai.data;

import org.shanghai.crawl.MetaCrawl;
import org.shanghai.util.FileUtil;
import org.shanghai.rdf.XMLTransformer;
import org.shanghai.data.Database;
import org.shanghai.util.TextUtil;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Property;

import java.io.InputStream;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.List;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
    @license http://www.apache.org/licenses/LICENSE-2.0
    @author Never Mind
    @title Simple Database Transporter
    @date 2013-11-16
*/
public class DBTransporter implements MetaCrawl.Transporter {

    public Database database;
    public Document document;

    private String table;
    private String idxQuery;
    private String dumpQuery;
    private XMLTransformer transformer = null;
    private String identifier;
    private DocumentBuilderFactory factory;

    private DBTransporter(String[] db, String[] idx) {
        database = new Database(db);
        idxQuery = FileUtil.readResource(idx[0]);
        dumpQuery = FileUtil.readResource(idx[1]);
        String x = idx[2];
        if (x==null) {
            this.transformer = new XMLTransformer();
        } else {
            this.transformer = new XMLTransformer(FileUtil.readResource(x));
        }
    }

    public DBTransporter(String[] db, String[] idx, int days) {
        this(db, idx);
        if (days>0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, 0-days);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            idxQuery = idxQuery.replace("<date>", df.format(cal.getTime()));
            //if (b) log(df.format(cal.getTime()));
            //if (days>0) {
            //    log(idxQuery);
            //}
        }
    }

    @Override
    public void create() {
        database.create();
        table = table(idxQuery);
        //log("guessed table [" + table + "]");
        if (transformer!=null)
            transformer.create();
        identifier = null;
        factory = DocumentBuilderFactory.newInstance();
    }

    @Override
    public void dispose() {
        database.dispose();
        if (transformer!=null)
            transformer.dispose();
    }

    private String table(String query) {
        int x = query.indexOf("from ")+5;
        int y1 = query.indexOf(" ",x);
        int y2 = query.indexOf(",",x);
        int y3 = query.indexOf("\n",x);
        if (y3>0) y1=y1>y3?y3:y1;
        if (y2>0) y1=y1>y2?y2:y1;
        y1=y1>0?y1:query.length();
        if (x>0 && y1>x)
            return query.substring(x,y1).trim();
        return "zero";
    }

    @Override
    public String probe() {
        String probe = database.getSingleText("select count(*) from " + table);
        //log("probe: " + probe);
        return table + " " + probe;
    }

    @Override
    public Resource read(String oid) {
        createDocument(oid);
        return transformer.transform(document);
    }

    //public Resource read(String oid, String uri) {
    //    Document document = createDocument(oid);
    //    Resource rc = transformer.transform(document, uri);
    //    return rc;
    //}

    @Override
    public List<String> getIdentifiers(int off, int limit) {
        String query = idxQuery + " limit " + off +"," + limit;
        return database.getColumn(query, limit);
    }

    @Override
    public int index(String str) {
        identifier = str;
        //return Integer.parseInt(super.probe());
        return 1;
    }

    @Override
    public Resource test(String resource) {
        log(idxQuery);
        return read(resource);
    }

    public String getSingleText(String query) {
        return database.getSingleText(query);
    }

    private Document createDocument(String oid) {
		try {
            DocumentBuilder builder =factory.newDocumentBuilder();
            document = builder.newDocument();
		} catch (ParserConfigurationException e) { log(e); }
        Element root = document.createElement("document");
        document.appendChild(root);
        String[] queries = dumpQuery.replace("<oid>",oid).split(";");
        for (String query : queries) {
            ResultSet rs = database.getResult(query);
            if (rs==null) {
                log("[" + query + "]");
                //continue;
            } else try {
                if (rs.isBeforeFirst()) { //if (rs.first())
                    Element results = document.createElement("resultset");
			        results.setAttribute("table", table(query));
                    root.appendChild(results);
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    while (rs.next()) {
                        Element row = document.createElement("row");
                        results.appendChild(row);
                        for (int ii = 1; ii <= colCount; ii++) {
                           String columnName = meta.getColumnName(ii);
                           //Object value = rs.getObject(ii);
                           String value = rs.getString(ii);
					       if (value!=null && value.toString().length()>0) {
                               Element node = document.createElement("field");
						       node.setAttribute("name", columnName);
                               node.appendChild(document.createTextNode(
                                 TextUtil.clean(value.toString()).trim()));
                               row.appendChild(node);
					       }
                        }
                    }
                }
            } catch(SQLException e) { log(e); }
        }
        return document;
    }

    private static final Logger logger =
                         Logger.getLogger(DBTransporter.class.getName());

    private void log(String msg) {
        logger.info(msg);
    }

    private void log(Exception ex) {
        logger.info(ex.toString());
        ex.printStackTrace();
    }
}