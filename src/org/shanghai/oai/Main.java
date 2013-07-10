package org.shanghai.oai;

import org.shanghai.oai.Importer;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
   @license http://www.apache.org/licenses/LICENSE-2.0
   @author Goetz Hatop <fb.com/goetz.hatop>
   @title A Command Line Interface for the Shanghai OAI Importer
   @date 2013-03-15
*/
public class Main extends org.shanghai.crawl.Main {

    protected OAIImporter importer;
    protected JenaCrawl crawl;

    //public void create() {
    //    importer = new OAIImporter(config).create();
    //    return this;
    //}

    public void dispose() {
	    if (importer!=null)
		    importer.dispose();
    }

    public int make(String[] args) {
        int argc = super.make(args);
        if (args.length==0)
            help();
        if (argc==args.length)
            return argc;
        if (config==null) {
            System.out.println("No ttl config.");
            return argc;
        }

        importer = new OAIImporter(config).create();
            
        if (args.length>argc && args[argc].endsWith("-oai")) {
            importer.make();
        } else if (args.length>argc && args[argc].endsWith("-otest")) {
            importer.test();
        } else if (args.length>argc && args[argc].endsWith("-republish")) {
            importer.show();
            // crawl = new JenaCrawl( config.getProperties() );
            // crawl.create();
            // crawl.crawl();
		    // crawl.dispose();
        } else {
        }

        return argc;
    }

    protected void help() {
        super.help();
        String usage = "\n" // "java org.shanghai.oai.Main \n"
                     + "   -otest : test oai setup \n"
                     + "   -oai   : start harvesting \n";
        System.out.print(usage);
    }

    public static void main(String[] args) {
	    Main main = new Main();
		main.make(args);
		main.dispose();
    }

}