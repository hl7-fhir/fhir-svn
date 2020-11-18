package org.hl7.fhir.tools.site;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.json.JSONUtil;

import com.google.gson.JsonObject;

public class WebSiteReleaseUpdater {
  private static final String START_HTML_MARKER = "<!--ReleaseHeader-->";
  private static final String END_HTML_MARKER = "<!--EndReleaseHeader-->";

  private String dir;
  private JsonObject ver;
  private String root;
  private JsonObject currentVer;
  private IniFile ini;
  private int total = 0;

  public WebSiteReleaseUpdater(String root, String dir, JsonObject ver, JsonObject currentVer) throws IOException {
    this.root = root;
    this.ini = new IniFile(Utilities.path(Utilities.getDirectoryForFile(root), "status.ini"));
    this.dir = dir;
    this.ver = ver;
    this.currentVer = currentVer;
  }

  public void execute(List<String> exempt) throws FileNotFoundException, IOException {
    System.out.print("v"+JSONUtil.str(ver, "version"));
    String statusMsg = generateStatusMessage(exempt);
    updateStatus(new File(dir), statusMsg, exempt, 0);
    System.out.println(" - "+total+" files updated");
    if (exempt == null) {
      FileUtils.copyFile(new File(Utilities.path(root, "fhir-pub.css")), new File(Utilities.path(dir, "fhir-pub.css")));
      FileUtils.copyFile(new File(Utilities.path(root, "external.png")), new File(Utilities.path(dir, "external.png")));
    }
  }

  private String generateStatusMessage(List<String> exempt) {
    String version = JSONUtil.str(ver, "version");
    String sequence = JSONUtil.str(ver, "sequence");
    String cSeq = JSONUtil.str(currentVer, "sequence");

    if (exempt != null)
      return "<p id=\"publish-box-current\">This is the current officially released version of FHIR, which is <a href=\"history.html\">"+sequence+
          "</a> (v"+version+"). For a full list of all versions, see the <a href=\"http://hl7.org/fhir/directory.cfml\">Directory of published versions <img src=\"{pd}external.png\" style=\"vertical-align: baseline\"></a>.</p>";
    
    String desc = JSONUtil.str(ver, "desc");
    String status = JSONUtil.str(ver, "status");
    boolean lastInSequence = Utilities.existsInList(status, "trial-use", "normative+trial-use");
    
    if (lastInSequence) {
      if (ver == currentVer)
        return "<p id=\"publish-box-milestone\">This page is part of FHIR "+sequence+" - the current version v"+version+" - in its permanent home (it will always be available at this URL). "+
             "For a full list of available versions, see the "+
             "<a href=\"http://hl7.org/fhir/directory.html\">Directory of published versions <img style=\"vertical-align: baseline\" src=\"{pd}external.png\"></a>.</p>";
      else
        return "<p id=\"publish-box-milestone\">This page is part of FHIR "+sequence+"  (v"+version+") in it's permanent home (it will always be available at this URL). "+
      "It has been superceded by <a href=\"http://hl7.org/fhir/{ref}\">"+cSeq+"</a>. For a full list of available versions, see the "+
      "<a href=\"http://hl7.org/fhir/directory.html\">Directory of published versions <img style=\"vertical-align: baseline\" src=\"{pd}external.png\"></a>.</p>";
    } else {
      return "<p id=\"publish-box-past\">This page was published as part of FHIR v"+version+": "+desc+". It has been superceded by <a href=\"http://hl7.org/fhir/{ref}\">"+cSeq+"</a>. For a full list of available versions, see the "+
          "<a href=\"http://hl7.org/fhir/directory.html\">Directory of published versions <img style=\"vertical-align: baseline\" src=\"{pd}external.png\"></a>.</p>";
    }
  }

  private String updateStatus(File d, String msg, List<String> exempt, int depth) throws FileNotFoundException, IOException {
    for (File f : d.listFiles()) {
      if (f.isDirectory()) {
        if (!Utilities.existsInList(f.getName(), "html", "uv", "us", "smart-app-launch", "quick") && (exempt == null || !exempt.contains(f.getAbsolutePath())) ) {
          String s = updateStatus(f, msg, null, depth+1);
          if (s != null)
            return s;
        }
      } else if (f.getName().endsWith(".html") || f.getName().endsWith(".htm")) {
        String src = TextFile.fileToString(f);
        String o = src;
        if (src.contains("<body") && !(src.contains("http-equiv=\"Refresh\"") || src.contains("http-equiv=\"refresh\""))) {
          int b = src.indexOf(START_HTML_MARKER);
          int e = src.lastIndexOf(END_HTML_MARKER);
          String rp = f.getAbsolutePath().substring(dir.length()+1);
          if (b == -1 || e == -1 || e < b) {
            throw new Error(f.getAbsolutePath().substring(root.length()+1)+" does not contain the html markers");
          } else {
            boolean hasCurrentMatch = new File(Utilities.path(root, rp)).exists();
            String fmsg = hasCurrentMatch ? msg.replace("{ref}", rp) : msg.replace("{ref}", "index.html");
            fmsg = fmsg.replace("{pd}", gen(depth));
            src = src.substring(0, b+START_HTML_MARKER.length()) + fmsg+src.substring(e);
          }
          b = src.indexOf("fhir-pub.css");
          if (b == -1) {
            b = src.indexOf("</head>");
            if (b == -1)
              throw new Error("unable to find </head> in "+rp);
            src = src.substring(0, b) + "  <link rel=\"stylesheet\" href=\"fhir-pub.css\"/>\r\n"+src.substring(b);
            b = src.indexOf("fhir-pub.css");
          }
          e = b;
          b = src.substring(0, b).lastIndexOf("\"");
          src = src.substring(0, b+1)+gen(depth)+src.substring(e);
          if (!src.equals(o)) {
            TextFile.stringToFile(src, f, false);
            total++;
          }
          
        }
      }
    }
    return null;
  }

  private String gen(int depth) {
    String res = "";
    for (int i = 0; i < depth; i++) {
      res = res + "../";
    }
    return res;
  }

  
}
