package edu.unm.health.biocomp.badapple;

import java.io.*;
import java.util.*;
import java.sql.*;
import java.text.DateFormat;

import org.apache.commons.cli.*; // CommandLine, CommandLineParser, HelpFormatter, OptionBuilder, Options, ParseException, PosixParser
import org.apache.commons.cli.Option.*; // Builder

import chemaxon.struc.*;
import chemaxon.formats.*;
import chemaxon.marvin.io.*;

import edu.unm.health.biocomp.hscaf.*;
import edu.unm.health.biocomp.util.db.*; // DBCon, pg_utils, mysql_utils, derby_utils
import edu.unm.health.biocomp.util.*; // time_utils
import edu.unm.health.biocomp.util.jre.*;

/**	Command line application for Badapple system.
	<br>
	Database engines supported:
	<ul>
	<li>PostgreSQL
	<ul>
	<li>PostgreSQL JDBC driver (org.postgresql.Driver).
	<li>Requires either (1) RDKit or (2) gNova OpenChord.
	</ul>
	<li>MySQL
	<ul>
	<li>MySQL JDBC driver (com.mysql.jdbc.Driver).
	<li>No chemical cartridge yet.
	</ul>
	<li>Derby
	</ul>
	<br>
	@author Jeremy J Yang
*/
public class badapple
{
  private static String APPNAME="BADAPPLE";
  private static String dbhost="localhost";
  private static Integer dbport=5432; //PostgreSql default
  private static String dbname="badapple";
  private static String dbdir="/tmp";
  private static String dbschema="public";
  private static String dbusr="www";
  private static String dbpw="foobar";
  private static String dbtype="postgres";
  private static String chemkit="rdkit";
  private static Integer maxatoms=50;
  private static Integer maxrings=5;
  private static Integer verbose=0;
  private static String smifmt="cxsmiles:u-L-l-e-d-D-p-R-f-w";
  private static String ifile=null;
  private static String ofile=null;
  private static String ofile_scaf=null;
  private static Boolean describedb=false;
  private static Boolean describescaf=false;
  private static Boolean process_mols=false;
  private static Integer scafid=null;
  private static Integer nmax=0;
  private static Integer nskip=0;
  private static Long scafid_min=null;
  private static Long scafid_max=null;

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) throws Exception
  {
    Options opts = new Options();
    OptionGroup operations = new OptionGroup();
    operations.addOption(Option.builder("describedb").desc("describe db").build());
     
    operations.addOption(Option.builder("describescaf").desc("describe specified scaffold").build());
    operations.addOption(Option.builder("process_mols").desc("process input molecules").build());
    operations.setRequired(true);
    opts.addOptionGroup(operations);
    String HELPHEADER = ("BADAPPLE - command line app for Badapple\nOperations: "+operations.toString());
    String HELPFOOTER = ("UNM Translational Informatics Division");

    opts.addOption(Option.builder("i").hasArg().argName("IFILE").desc("input molecules").build());
    opts.addOption(Option.builder("o").hasArg().argName("OFILE").desc("output molecules, w/ scores").build());
    opts.addOption(Option.builder("scafid").hasArg().desc("scaffold ID").build());
    opts.addOption(Option.builder("dbtype").hasArg().desc("db type (postgres|mysql|derby) ["+dbtype+"]").build());
    opts.addOption(Option.builder("chemkit").hasArg().desc("chemical cartridge (rdkit|openchord) ["+chemkit+"]").build());
    opts.addOption(Option.builder("dbhost").hasArg().desc("db host ["+dbhost+"]").build());
    opts.addOption(Option.builder("dbdir").hasArg().desc("db dir (Derby only) ["+dbdir+"]").build());
    opts.addOption(Option.builder("dbname").hasArg().desc("db name ["+dbname+"]").build());
    opts.addOption(Option.builder("dbschema").hasArg().desc("db schema ["+dbschema+"] (postgres)").build());
    opts.addOption(Option.builder("dbusr").hasArg().desc("db user ["+dbusr+"]").build());
    opts.addOption(Option.builder("dbpw").hasArg().desc("db password").build());
    opts.addOption(Option.builder("dbport").hasArg().type(Integer.class).desc("db port ["+dbport+"]").build());
    opts.addOption(Option.builder("maxatoms").hasArg().type(Integer.class).desc("max atom count of input mol ["+maxatoms+"]").build());
    opts.addOption(Option.builder("maxrings").hasArg().type(Integer.class).desc("max ring count of input mol ["+maxrings+"]").build());
    opts.addOption(Option.builder("nmax").hasArg().type(Integer.class).desc("quit after NMAX molecules").build());
    opts.addOption(Option.builder("nskip").hasArg().type(Integer.class).desc("skip NSKIP molecules").build());
    opts.addOption(Option.builder("scafid_min").desc("min scaf ID to calculate/annotate").build());
    opts.addOption(Option.builder("scafid_max").desc("max scaf ID to calculate/annotate").build());

    opts.addOption("v", "verbose", false, "verbose.");
    opts.addOption("vv", "vverbose", false, "very verbose.");
    opts.addOption("vvv", "vvverbose", false, "very very verbose.");
    opts.addOption("h", "help", false, "Show this help.");
    HelpFormatter helper = new HelpFormatter();
    CommandLineParser clp = new PosixParser();
    CommandLine cl = null;
    try {
      cl = clp.parse(opts, args);
    } catch (ParseException e) {
      helper.printHelp(APPNAME, HELPHEADER, opts, e.getMessage(), true);
      System.exit(0);
    }
  
    //Operations:
    if (cl.hasOption("describedb")) describedb = true;
    if (cl.hasOption("describescaf")) describescaf = true;
    if (cl.hasOption("process_mols")) process_mols = true;

    if (cl.hasOption("i")) ifile = cl.getOptionValue("i");
    if (cl.hasOption("o")) ofile = cl.getOptionValue("o");
    if (cl.hasOption("dbhost")) dbhost = cl.getOptionValue("dbhost");
    if (cl.hasOption("dbname")) dbname = cl.getOptionValue("dbname");
    if (cl.hasOption("dbschema")) dbschema = cl.getOptionValue("dbschema");
    if (cl.hasOption("dbusr")) dbusr = cl.getOptionValue("dbusr");
    if (cl.hasOption("dbpw")) dbpw = cl.getOptionValue("dbpw");
    if (cl.hasOption("dbdir")) dbdir = cl.getOptionValue("dbdir");
    if (cl.hasOption("dbport")) dbport = (Integer)(cl.getParsedOptionValue("dbport"));
    if (cl.hasOption("chemkit")) chemkit = cl.getOptionValue("chemkit");
    if (cl.hasOption("dbtype")) dbtype = cl.getOptionValue("dbtype");
    if (cl.hasOption("maxrings")) maxrings = (Integer)(cl.getParsedOptionValue("maxrings"));
    if (cl.hasOption("maxatoms")) maxatoms = (Integer)(cl.getParsedOptionValue("maxatoms"));
    if (cl.hasOption("nmax")) nmax = (Integer)(cl.getParsedOptionValue("nmax"));
    if (cl.hasOption("nskip")) nskip = (Integer)(cl.getParsedOptionValue("nskip"));
    if (cl.hasOption("scafid")) scafid = (Integer)(cl.getParsedOptionValue("scafid"));
    if (cl.hasOption("scafid_min")) scafid_min = (Long)(cl.getParsedOptionValue("scafid_min"));
    if (cl.hasOption("scafid_max")) scafid_max = (Long)(cl.getParsedOptionValue("scafid_max"));
    if (cl.hasOption("vvv")) verbose = 3;
    else if (cl.hasOption("vv")) verbose = 2;
    else if (cl.hasOption("v")) verbose = 1;
    if (cl.hasOption("h")) {
      helper.printHelp(APPNAME, HELPHEADER, opts, HELPFOOTER, true);
      System.exit(0);
    }

    if (verbose>0)
    {
      System.err.println("JRE_VERSION: "+JREUtils.JREVersion());
      System.err.println("JChem version: "+com.chemaxon.version.VersionInfo.getVersion());
    }

    MolImporter molReader=null;
    if (ifile!=null)
    {
      if (!(new File(ifile).exists())) 
      helper.printHelp(APPNAME, HELPHEADER, opts, ("Non-existent input file: "+ifile), true);
      molReader = new MolImporter(ifile);
    }

    MolExporter molWriter=null;
    if (ofile!=null)
    {
      String ofmt = MFileFormatUtil.getMostLikelyMolFormat(ofile);
      if (ofmt.equals("smiles")) ofmt="smiles:+n-a"; //Kekule for compatibility
      molWriter = new MolExporter(new FileOutputStream(ofile),ofmt);
    } else {
      molWriter = new MolExporter(System.out,"sdf");
    }

    String dbname_full = (dbtype.equals("derby")?(dbdir+"/"+dbname):dbname);

    DBCon dbcon = null;
    try { dbcon = new DBCon(dbtype,dbhost,dbport,dbname_full,dbusr,dbpw); }
    catch (Exception e) {
      helper.printHelp(APPNAME, HELPHEADER, opts, ("DB ("+dbtype+") error; "+e.getMessage()), true);
      System.exit(0);
    }
    if (dbcon==null) {
      helper.printHelp(APPNAME, HELPHEADER, opts, ("DB ("+dbtype+") connection failed."), true);
      System.exit(0);
    } else if (verbose>0) {
      System.err.println("DB ("+dbtype+") connection ok :"+(dbtype.equals("derby")?"":(dbhost+":"+dbport+":"))+dbname_full);
      if (verbose>1) System.err.println(dbcon.serverStatusTxt());
    }

    if (describedb)
    {
      System.out.println("database: ("+dbtype+") "+dbname_full);
      System.out.println(badapple_utils.DBDescribeTxt(dbcon, dbschema));
    }
    else if (describescaf)
    {
      if (scafid==null) 
        helper.printHelp(APPNAME, HELPHEADER, opts, ("ERROR: -scafid ID required for -describescaf."), true);
      System.out.println("database: "+dbname_full+"\nScafID: "+describescaf);
      System.out.println(badapple_utils.ScaffoldDescribeTxt(dbcon, dbschema, chemkit, scafid, verbose));
    }
    else if (process_mols)
    {
      if (molReader==null)
        helper.printHelp(APPNAME, HELPHEADER, opts, ("ERROR: -i IFILE required for -process_mols."), true);
      badapple_utils.ProcessMols(dbcon, dbschema, chemkit, molReader, molWriter, nskip, nmax, maxatoms, maxrings, verbose);
    }
    else
    {
      helper.printHelp(APPNAME, HELPHEADER, opts, ("ERROR: no operation specified."), true);
    }
  }
}
