package org.hl7.fhir.definitions.validation;

import java.io.IOException;
/*
 Copyright (c) 2011+, HL7, Inc
 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification, 
 are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this 
 list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 this list of conditions and the following disclaimer in the documentation 
 and/or other materials provided with the distribution.
 * Neither the name of HL7 nor the names of its contributors may be used to 
 endorse or promote products derived from this software without specific 
 prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 POSSIBILITY OF SUCH DAMAGE.

 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.definitions.model.BindingSpecification;
import org.hl7.fhir.definitions.model.BindingSpecification.BindingMethod;
import org.hl7.fhir.definitions.model.BindingSpecification.ElementType;
import org.hl7.fhir.definitions.model.Compartment;
import org.hl7.fhir.definitions.model.DefinedCode;
import org.hl7.fhir.definitions.model.DefinedStringPattern;
import org.hl7.fhir.definitions.model.Definitions;
import org.hl7.fhir.definitions.model.ElementDefn;
import org.hl7.fhir.definitions.model.ImplementationGuideDefn;
import org.hl7.fhir.definitions.model.LogicalModel;
import org.hl7.fhir.definitions.model.Operation;
import org.hl7.fhir.definitions.model.Operation.OperationExample;
import org.hl7.fhir.definitions.model.ResourceDefn;
import org.hl7.fhir.definitions.model.SearchParameterDefn;
import org.hl7.fhir.definitions.model.SearchParameterDefn.SearchType;
import org.hl7.fhir.definitions.model.TypeDefn;
import org.hl7.fhir.definitions.model.W5Entry;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.igtools.spreadsheets.MappingSpace;
import org.hl7.fhir.igtools.spreadsheets.TypeRef;
import org.hl7.fhir.r5.context.CanonicalResourceManager;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r5.model.Enumerations.BindingStrength;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r5.terminologies.ValueSetUtilities;
import org.hl7.fhir.r5.utils.Translations;
import org.hl7.fhir.validation.BaseValidator;
import org.hl7.fhir.utilities.StandardsStatus;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.validation.ValidationMessage.Source;


/** todo
 * check code lists used in Codings have displays
 * 
 * @author Grahame
 *
 */
public class ResourceValidator extends BaseValidator {

  public static class SearchParameterGroup {
    private String name;
    private String type;
    private List<String> resources = new ArrayList<String>();
     
  }
  
  public static class Usage {
    public Set<SearchParameterDefn.SearchType> usage = new HashSet<SearchParameterDefn.SearchType>();
  }

  public static class UsageT {
    public Set<String> usage = new HashSet<String>();
  }

  private Definitions definitions;
  private PatternFinder patternFinder;
  private final Map<String, Usage> usages = new HashMap<String, Usage>();
  private final Map<String, Integer> names = new HashMap<String, Integer>();
  private final Map<SearchType, UsageT> usagest = new HashMap<SearchType, UsageT>();
  private final Map<String, SearchParameterGroup> spgroups = new HashMap<String, SearchParameterGroup>();
  private Translations translations;
  private final CanonicalResourceManager<CodeSystem> codeSystems;
  private SpellChecker speller;
  private int maxElementLength;
  private List<FHIRPathUsage> fpUsages;
  private List<String> suppressedMessages;
  private IWorkerContext context;
  private Set<String> txurls = new HashSet<String>();
  
//  private Map<String, Integer> typeCounter = new HashMap<String, Integer>();

	public ResourceValidator(Definitions definitions, Translations translations, CanonicalResourceManager<CodeSystem> map, String srcFolder, List<FHIRPathUsage> fpUsages, List<String> suppressedMessages, IWorkerContext context) throws IOException {
		super(context);
		source = Source.ResourceValidator;
		this.definitions = definitions;
		this.translations = translations;
		this.codeSystems = map;
		this.fpUsages = fpUsages;
		this.context = context;
	  patternFinder = new PatternFinder(definitions);
		speller = new SpellChecker(srcFolder, definitions);
		int l = 0;
		for (String n : definitions.getTypes().keySet())
		  l = Math.max(l, n.length());
    for (String n : definitions.getPrimitives().keySet())
      l = Math.max(l, n.length());
    for (String n : definitions.getConstraints().keySet())
      l = Math.max(l, n.length());
    for (String n : definitions.getInfrastructure().keySet())
      l = Math.max(l, n.length());
		maxElementLength = (60 - 7) - l;
		this.suppressedMessages = suppressedMessages;
	}

  // public void setConceptDomains(List<ConceptDomain> conceptDomains) {
	// this.conceptDomains = conceptDomains;
	// }
	//
	// public void defineReference(String name) {
	// this.resources.add(name);
	// }
	//
	// public void setDataTypes(String[] names) throws Exception {
	// TypeParser tp = new TypeParser();
	// for (String tn : names) {
	// datatypes.addAll(tp.parse(tn));
	// }
	// }

  public void checkStucture(List<ValidationMessage> errors, String name, ElementDefn structure) throws Exception {
    rule(errors, IssueType.STRUCTURE, structure.getName(), name.length() > 1 && Character.isUpperCase(name.charAt(0)), "Resource Name must start with an uppercase alpha character");
    ResourceDefn fakeParent = new ResourceDefn();
    fakeParent.setRoot((TypeDefn) structure);
    fakeParent.setWg(definitions.getWorkgroups().get("fhir"));
    fakeParent.setFmmLevel(fakeParent.getRoot().getFmmLevel());
    fakeParent.setStatus(fakeParent.getRoot().getStandardsStatus());
    if (fakeParent.getStatus() == StandardsStatus.NORMATIVE)
     fakeParent.setNormativeVersion("4.0.0");
    checkElement(errors, structure.getName(), structure, fakeParent, null, true, false, hasSummary(structure), new ArrayList<String>(), true, structure.getStandardsStatus());
  }
  
  private boolean hasSummary(ElementDefn structure) {
    if (structure.isSummary())
      return true;
    for (ElementDefn e : structure.getElements()) {
      if (hasSummary(e))
        return true;
    }
    return false;
  }

  public List<ValidationMessage> checkStucture(String name, ElementDefn structure) throws Exception {
    List<ValidationMessage> errors = new ArrayList<ValidationMessage>();
    checkStucture(errors, name, structure);
    return errors;
  }
  
  protected boolean rule(List<ValidationMessage> errors, IssueType type, String path, boolean b, String msg) {
    String rn = path.contains(".") ? path.substring(0, path.indexOf(".")) : path;
    return super.ruleHtml(errors, type, path, b, msg, "<a href=\""+(rn.toLowerCase())+".html\">"+rn+"</a>: "+Utilities.escapeXml(msg));
  }

  public void check(List<ValidationMessage> errors, String name, ResourceDefn rd) throws Exception {
    for (String s : rd.getHints())
      hint(errors, IssueType.INFORMATIONAL, rd.getName(), false, s);

    rule(errors, IssueType.STRUCTURE, rd.getName(), !name.equals("Metadata"), "The name 'Metadata' is not a legal name for a resource");
    rule(errors, IssueType.STRUCTURE, rd.getName(), !name.equals("History"), "The name 'History' is not a legal name for a resource");
    rule(errors, IssueType.STRUCTURE, rd.getName(), !name.equals("Tag"), "The name 'Tag' is not a legal name for a resource");
    rule(errors, IssueType.STRUCTURE, rd.getName(), !name.equals("Tags"), "The name 'Tags' is not a legal name for a resource");
    rule(errors, IssueType.STRUCTURE, rd.getName(), !name.equals("MailBox"), "The name 'MailBox' is not a legal name for a resource");
    rule(errors, IssueType.STRUCTURE, rd.getName(), !name.equals("Validation"), "The name 'Validation' is not a legal name for a resource");
    rule(errors, IssueType.REQUIRED,  rd.getName(), name.equals("Parameters") || translations.hasTranslation(name), "The name '"+name+"' is not found in the file translations.xml");
    rule(errors, IssueType.STRUCTURE, rd.getName(), name.length() > 1 && Character.isUpperCase(name.charAt(0)), "Resource Name must start with an uppercase alpha character");
    rule(errors, IssueType.STRUCTURE, rd.getName(), !Utilities.noString(rd.getFmmLevel()), "Resource must have a maturity level");
    rule(errors, IssueType.REQUIRED,  rd.getName(), rd.getRoot().getElements().size() > 0, "A resource must have at least one element in it before the build can proceed"); // too many downstream issues in the parsers, and it would only happen as a transient thing when designing the resources
    rule(errors, IssueType.REQUIRED,  rd.getName(), rd.getWg() != null, "A resource must have a designated owner"); // too many downstream issues in the parsers, and it would only happen as a transient thing when designing the resources
    rule(errors, IssueType.REQUIRED,  rd.getName(), !Utilities.noString(rd.getRoot().getW5()), "A resource must have a W5 category"); 
    rd.getRoot().setMinCardinality(0);
    rd.getRoot().setMaxCardinality(Integer.MAX_VALUE);
    // pattern related rules
    buildW5Mappings(rd.getRoot(), true);    
    if ((isWorkflowPattern(rd, "Event") || isWorkflowPattern(rd, "Request")) && hasPatient(rd)) {
      rule(errors, IssueType.STRUCTURE, rd.getName(), rd.getSearchParams().containsKey("patient"), "An 'event' or 'request' resource must have a search parameter 'patient'");
    }
    
    if (suppressedwarning(errors, IssueType.REQUIRED, rd.getName(), hasW5Mappings(rd) || rd.getName().equals("Binary") || rd.getName().equals("OperationOutcome") || rd.getName().equals("Parameters"), "A resource must have w5 mappings")) {
      String w5Order = listW5Elements(rd);
      String w5CorrectOrder = listW5Correct(rd);
      if (!w5Order.equals(w5CorrectOrder)) {
        warning(errors, IssueType.REQUIRED, rd.getName(), false, "Resource elements are out of order. The correct order is '"+w5CorrectOrder+"' but the actual order is '"+w5Order+"'");
//        System.out.println("Resource "+parent.getName()+": elements are out of order. The correct order is '"+w5CorrectOrder+"' but the actual order is '"+w5Order+"'");
      }
      if (!Utilities.noString(rd.getProposedOrder())) {
        w5Order = listW5Elements(rd, rd.getProposedOrder());
        if (!w5Order.equals(w5CorrectOrder)) {
          rule(errors, IssueType.REQUIRED, rd.getName(), false, "Proposed Resource elements are out of order. The correct order is '"+w5CorrectOrder+"' but the proposed order is '"+w5Order+"'");
        } else 
          System.out.println("Proposed order for "+rd.getName()+": build order ok");
      }
    }
    if (Utilities.noString(rd.getEnteredInErrorStatus()))
      if (hasStatus(rd, "entered-in-error"))
        rd.setEnteredInErrorStatus(".status = entered-in-error");
      else if (hasStatus(rd, "retired"))
        rd.setEnteredInErrorStatus(".status = retired");
      else if (hasActivFalse(rd))
        rd.setEnteredInErrorStatus(".active = false");
      else 
        warning(errors, IssueType.REQUIRED,  rd.getName(), false, "A resource must have an 'entered in error' status"); // too many downstream issues in the parsers, and it would only happen as a transient thing when designing the resources
        
    String s = rd.getRoot().getMapping(Definitions.RIM_MAPPING);
    hint(errors, IssueType.REQUIRED, rd.getName(), !Utilities.noString(s), "RIM Mapping is required");

    for (Operation op : rd.getOperations()) {
      warning(errors, IssueType.BUSINESSRULE, rd.getName()+".$"+op.getName(), hasOpExample(op.getExamples(), false), "Operation must have an example request");
      warning(errors, IssueType.BUSINESSRULE, rd.getName()+".$"+op.getName(), hasOpExample(op.getExamples(), true), "Operation must have an example response");
    }
    List<String> vsWarns = new ArrayList<String>();
    int vsWarnings = checkElement(errors, rd.getName(), rd.getRoot(), rd, null, s == null || !s.equalsIgnoreCase("n/a"), false, hasSummary(rd.getRoot()), vsWarns, true, rd.getStatus());
    
    if (!resourceIsTechnical(name)) { // these are exempt because identification is tightly managed
      ElementDefn id = rd.getRoot().getElementByName(definitions, "identifier", true, false);
      if (id == null) 
        warning(errors, IssueType.STRUCTURE, rd.getName(), false, "All resources should have an identifier");
      else 
        rule(errors, IssueType.STRUCTURE, rd.getName(), id.typeCode().equals("Identifier"), "If a resource has an element named identifier, it must have a type 'Identifier'");
    }
    rule(errors, IssueType.STRUCTURE, rd.getName(), rd.getRoot().getElementByName(definitions, "text", true, false) == null, "Element named \"text\" not allowed");
    rule(errors, IssueType.STRUCTURE, rd.getName(), rd.getRoot().getElementByName(definitions, "contained", true, false) == null, "Element named \"contained\" not allowed");
    if (rd.getRoot().getElementByName(definitions, "subject", true, false) != null && rd.getRoot().getElementByName(definitions, "subject", true, false).typeCode().startsWith("Reference"))
      rule(errors, IssueType.STRUCTURE, rd.getName(), rd.getSearchParams().containsKey("subject"), "A resource that contains a subject reference must have a search parameter 'subject'");
    ElementDefn ped = rd.getRoot().getElementByName(definitions, "patient", true, false);
    if (ped != null && ped.typeCode().startsWith("Reference")) {
      SearchParameterDefn spd = rd.getSearchParams().get("patient");
      if (rule(errors, IssueType.STRUCTURE, rd.getName(), spd != null, "A resource that contains a patient reference must have a search parameter 'patient'"))
        rule(errors, IssueType.STRUCTURE, rd.getName(), 
            (spd.getTargets().size() ==1 && spd.getTargets().contains("Patient")) ||  (ped.getTypes().get(0).getParams().size() == 1 && ped.getTypes().get(0).getParams().get(0).equals("Patient")), 
            "A Patient search parameter must only refer to patient");
    }
    ElementDefn sed = rd.getRoot().getElementByName(definitions, "subject", true, false);
    if (sed != null && sed.typeCode().startsWith("Reference") && sed.typeCode().contains("Patient"))
      warning(errors, IssueType.STRUCTURE, rd.getName(), rd.getSearchParams().containsKey("patient"), "A resource that contains a subject that can be a patient reference must have a search parameter 'patient'");
    if (rd.getRoot().getElementByName(definitions, "identifier", true, false) != null) {
      warning(errors, IssueType.STRUCTURE, rd.getName(), rd.getSearchParams().containsKey("identifier"), "A resource that contains an identifier must have a search parameter 'identifier'");
    }
    if (rd.getRoot().getElementByName(definitions, "status", true, false) != null) {
      hint(errors, IssueType.STRUCTURE, rd.getName(), rd.getSearchParams().containsKey("status"), "A resource that contains a status element must have a search parameter 'status'"); // todo: change to a warning post STU3
    }
    if (rd.getRoot().getElementByName(definitions, "url", true, false) != null) {
      warning(errors, IssueType.STRUCTURE, rd.getName(), rd.getSearchParams().containsKey("url"), "A resource that contains a url element must have a search parameter 'url'");
    }
    checkSearchParams(errors, rd);
    for (Operation op : rd.getOperations()) {
      rule(errors, IssueType.STRUCTURE, rd.getName()+"/$"+op.getName(), !parentHasOp(rd.getRoot().typeCode(), op.getName()), "Duplicate Operation Name $"+op.getName()+" on "+rd.getName()); 
    }
    
    for (Compartment c : definitions.getCompartments()) {
      if (rule(errors, IssueType.STRUCTURE, rd.getName(), name.equals("Parameters") || c.getResources().containsKey(rd), "Resource not entered in resource map for compartment '"+c.getTitle()+"' (compartments.xml)")) {
        String param = c.getResources().get(rd);
        if (!Utilities.noString(param)) {
//          rule(errors, IssueType.STRUCTURE, parent.getName(), param.equals("{def}") || parent.getSearchParams().containsKey(c.getName()), "Resource "+parent.getName()+" in compartment " +c.getName()+" must have a search parameter named "+c.getName().toLowerCase()+")");
          for (String p : param.split("\\|")) {
            String pn = p.trim();
            if (pn.contains("."))
              pn = pn.substring(0, pn.indexOf("."));
            rule(errors, IssueType.STRUCTURE, rd.getName(), Utilities.noString(pn) || pn.equals("{def}") || rd.getSearchParams().containsKey(pn), "Resource "+rd.getName()+" in compartment " +c.getName()+": parameter "+param+" was not found ("+pn+")");
          }
        }
      }
    }
    // Remove suppressed messages
    List<ValidationMessage> suppressed = new ArrayList<ValidationMessage>();
    for (ValidationMessage em : errors) {
      if (isSuppressedMessage(em.getDisplay())) {
        suppressed.add(em);
      }
    }
    errors.removeAll(suppressed);
    
    // last check: if maturity level is 
    int warnings = 0;
    int hints = 0;
    for (ValidationMessage em : errors) {
      if (em.getLevel() == IssueSeverity.WARNING)
        warnings++;
      else if (em.getLevel() == IssueSeverity.INFORMATION)
        hints++;
    }
    boolean ok = warnings == 0 || "0".equals(rd.getFmmLevel());
 
    ok = hints == 0 || Integer.parseInt(rd.getFmmLevel()) < 3;
 
    
//    if (isInterface(rd.getRoot().typeCode())) {
//      checkInterface(errors, rd, definitions.getBaseResources().get(rd.getRoot().typeCode()));
//    }
	}

  private int checkInterface(List<ValidationMessage> errors, ResourceDefn self, ResourceDefn iface) {
    int lastIndex = -1;
    if (isInterface(iface.getRoot().typeCode())) {
      lastIndex = checkInterface(errors, self, definitions.getBaseResources().get(iface.getRoot().typeCode()));
    }
    for (ElementDefn ei : iface.getRoot().getElements()) {
      ElementDefn es = null;
      for (ElementDefn t : self.getRoot().getElements()) {
        if (ei.getName().equals(t.getName())) {
          es = t;
        }
      }
      List<String> path = new ArrayList<>();
      path.add(self.getName());
      if (rule(errors, IssueType.BUSINESSRULE, path, es != null, "[ic]: No definition for element '"+ei.getName()+" defined in "+iface.getName())) {
        int index = self.getRoot().getElements().indexOf(es);
        path.add(ei.getName());
        rule(errors, IssueType.BUSINESSRULE, path, ei.getMinCardinality() <= es.getMinCardinality(), "[ic]: Min cardinality for "+self.getName()+"."+ei.getName()+" is "+es.getMinCardinality()+" but interface has "+ei.getMinCardinality());
        rule(errors, IssueType.BUSINESSRULE, path, ei.getMaxCardinality() >= es.getMaxCardinality(), "[ic]: Max cardinality for "+self.getName()+"."+ei.getName()+" is "+es.getMaxCardinality()+" but interface has "+ei.getMaxCardinality());
        for (TypeRef t : es.getTypes()) {
          rule(errors, IssueType.BUSINESSRULE, path, ei.hasType(t.getName()), "[ic]: The type "+t.getName()+" is not valid in the interface");
        }
        rule(errors, IssueType.BUSINESSRULE, path, lastIndex < index, "[ic]: Out of order "+self.getName()+"."+ei.getName()+" ("+lastIndex+" / "+index);
        lastIndex = index;
      }
    }
    return lastIndex;
  }

  private boolean isInterface(String typeCode) {
    return definitions.getBaseResources().containsKey(typeCode) && definitions.getBaseResources().get(typeCode).isInterface();
  }

  public void checkSearchParams(List<ValidationMessage> errors, ResourceDefn rd) {
    for (org.hl7.fhir.definitions.model.SearchParameterDefn p : rd.getSearchParams().values()) {
      if (!usages.containsKey(p.getCode()))
        usages.put(p.getCode(), new Usage());
      usages.get(p.getCode()).usage.add(p.getType());
      if (!usagest.containsKey(p.getType()))
        usagest.put(p.getType(), new UsageT());
      String spgn = p.getCode()+"||"+p.getType().toString();
      if (!spgroups.containsKey(spgn)) {
        SearchParameterGroup spg = new SearchParameterGroup();
        spg.name = p.getCode();
        spg.type = p.getType().toString();
        spgroups.put(spgn, spg);
      }
      spgroups.get(spgn).resources.add(rd.getName());
      rule(errors, IssueType.FORBIDDEN, rd.getName(), checkNamingPattern(rd.getName(), p.getCode()), "Search Parameter name is not valid - must use lowercase letters with '_' between words");
      rule(errors, IssueType.STRUCTURE, rd.getName(), !p.getCode().equals("filter"), "Search Parameter Name cannot be 'filter')");
      rule(errors, IssueType.STRUCTURE, rd.getName(), !p.getCode().contains("."), "Search Parameter Names cannot contain a '.' (\""+p.getCode()+"\")");
      rule(errors, IssueType.STRUCTURE, rd.getName(), !p.getCode().equalsIgnoreCase("id"), "Search Parameter Names cannot be named 'id' (\""+p.getCode()+"\")");
      warning(errors, IssueType.STRUCTURE, rd.getName(), !stringMatches(p.getCode(), "id", "lastUpdated", "tag", "profile", "security", "text", "content", "list", "query"), "Search Parameter Names cannot be named one of the reserved names (\""+p.getCode()+"\")");
      hint(errors, IssueType.STRUCTURE, rd.getName(), searchNameOk(p.getCode()), "Search Parameter name '"+p.getCode()+"' does not follow the style guide");
      rule(errors, IssueType.STRUCTURE, rd.getName(), p.getCode().equals(p.getCode().toLowerCase()) || p.getCode().equals("_lastUpdated"), "Search Parameter Names should be all lowercase (\""+p.getCode()+"\")");

      try {
        if (!Utilities.noString(p.getExpression()))
          fpUsages.add(new FHIRPathUsage(rd.getName()+"::"+p.getCode(), rd.getName(), rd.getName(), p.getDescription(), p.getExpression().replace("[x]", "")));
        for (String path : p.getPaths()) {
          ElementDefn e;
          String pp = trimIndexes(path);
          e = rd.getRoot().getElementForPath(pp, definitions, "Resolving Search Parameter Path", true, false);
          List<TypeRef> tlist;
          if (pp.endsWith("."+e.getName()))
            tlist = e.getTypes();
          else {
            tlist = new ArrayList<TypeRef>();
            for (TypeRef t : e.getTypes())
              if (pp.endsWith(Utilities.capitalize(t.getName())))
                tlist.add(t);
          }
          for (TypeRef t : tlist) {
            String tn = t.getName();
            if (definitions.getSearchRules().containsKey(tn) && definitions.getSearchRules().get(tn).contains(p.getType().name())) { 
              if (definitions.getConstraints().containsKey(tn))
                tn = definitions.getConstraints().get(tn).getBaseType();
              else if (definitions.getPrimitives().containsKey(tn) && definitions.getPrimitives().get(tn) instanceof DefinedStringPattern && !tn.equals("code") && !tn.equals("canonical")) 
                tn = ((DefinedStringPattern) definitions.getPrimitives().get(tn)).getBase();            
              usagest.get(p.getType()).usage.add(tn);
            } else 
              warning(errors, IssueType.STRUCTURE, rd.getName(), tlist.size() > 1, "Search Parameter "+p.getCode()+" : "+p.getType().name()+" type illegal for "+path+" : "+tn+" ("+e.typeCode()+")");      
          }
        }
      } catch (Exception e1) {
      }
      try {
        if (p.getType() == SearchType.reference) {
          for (String path : p.getPaths()) {
            ElementDefn e;
            String pp = trimIndexes(path);
            e = rd.getRoot().getElementForPath(pp, definitions, "Resolving Search Parameter Path", true, false);
            for (TypeRef t : e.getTypes()) {
              if (t.getName().equals("Reference") || t.getName().equals("canonical") ) {
                for (String pn : t.getParams()) {
                  if (definitions.hasLogicalModel(pn))
                    p.getTargets().addAll(definitions.getLogicalModel(pn).getImplementations());
                  else
                    p.getTargets().add(pn);
                }
              }
            }
          }
        }
        if (p.getType() == SearchType.uri) {
          for (String path : p.getPaths()) {
            ElementDefn e;
            String pp = trimIndexes(path);
            e = rd.getRoot().getElementForPath(pp, definitions, "Resolving Search Parameter Path", true, false);
            for (TypeRef t : e.getTypes()) {
              if (t.getName().equals("Reference")/* || t.getName().equals("canonical")*/ ) {
                rule(errors, IssueType.STRUCTURE, rd.getName(), false, "Parameters of type uri cannot refer to the types Reference or canonical ("+p.getCode()+")");
              }
            }
          }
        }
      } catch (Exception e1) {
        rule(errors, IssueType.STRUCTURE, rd.getName(), false, e1.getMessage());
      }
    }
  }

  private boolean checkNamingPattern(String rn, String pn) {
    if (Utilities.existsInList(pn, "_lastUpdated", "_revinclude", "_containedType"))
      return true;
    
    return pn.toLowerCase().equals(pn);
  }

  private void buildW5Mappings(ElementDefn ed, boolean root) {
    if (!root && !Utilities.noString(ed.getW5())) {
      ed.getMappings().put("http://hl7.org/fhir/fivews", translateW5(ed.getW5()));
    }
    for (ElementDefn child : ed.getElements()) {
      buildW5Mappings(child, false);
    }
    
  }

  private String translateW5(String w5) {
    if ("id".equals(w5)) return "FiveWs.identifier";
    if ("id.version".equals(w5)) return "FiveWs.version";
    if ("status".equals(w5)) return "FiveWs.status";
    if ("class".equals(w5)) return "FiveWs.class";
    if ("grade".equals(w5)) return "FiveWs.grade";
    if ("what".equals(w5)) return "FiveWs.what[x]";
    if ("who.focus".equals(w5)) return "FiveWs.subject[x]";
    if ("context".equals(w5)) return "FiveWs.context";
    if ("when.init".equals(w5)) return "FiveWs.init";
    if ("when.planned".equals(w5)) return "FiveWs.planned";
    if ("when.done".equals(w5)) return "FiveWs.done[x]";
    if ("when.recorded".equals(w5)) return "FiveWs.recorded";
    if ("who.author".equals(w5)) return "FiveWs.author";
    if ("who.source".equals(w5)) return "FiveWs.source";
    if ("who.actor".equals(w5)) return "FiveWs.actor";
    if ("who.cause".equals(w5)) return "FiveWs.cause";
    if ("who.witness".equals(w5)) return "FiveWs.witness";
    if ("who".equals(w5)) return "FiveWs.who";
    if ("where".equals(w5)) return "FiveWs.where[x]";
    if ("why".equals(w5)) return "FiveWs.why[x]";

    return null;
  }

  private boolean isSuppressedMessage(String message) {
    for (String s : suppressedMessages)
      if (s.contains(message))
        return true;
    return false;
  }

  private boolean hasPatient(ResourceDefn rd) {
    for (ElementDefn child : rd.getRoot().getElements()) {
      if (child.getName().equals("patient"))
        return true;
      if (child.getName().equals("subject")) {
        for (TypeRef tr : child.getTypes()) {
          if (tr.getName().equals("Reference") && tr.getParams().contains("Patient"))
            return true;
        }
      }
    }
    return false;
  }

  private boolean isWorkflowPattern(ResourceDefn rd, String code) {
    ElementDefn ed = rd.getRoot();
    String wfm = ed.getMapping("http://hl7.org/fhir/workflow");
    return wfm != null && code.equals(wfm);
  }

  private String listW5Elements(ResourceDefn parent, String proposedOrder) {
    String[] names = proposedOrder.split("\\,"); 
    List<String> items = new ArrayList<String>();
    for (String n : names) {
      ElementDefn e = parent.getRoot().getElementByName(definitions, n, true, false);
      if (e == null)
        throw new Error("Unable to resolve element in proposed order: "+n);
      if (!Utilities.noString(e.getW5()))
        items.add(e.getName()+"(="+e.getW5()+")");
    }
    return items.toString();
  }

  private String listW5Correct(ResourceDefn parent) {
    List<String> items = new ArrayList<String>();
    for (W5Entry w5 : definitions.getW5list()) {
      for (ElementDefn e : parent.getRoot().getElements()) {
        if (w5.getCode().equals(e.getW5()))
          items.add(e.getName()+"(="+e.getW5()+")");
      }
    }
    return items.toString();
  }

  private String listW5Elements(ResourceDefn parent) {
    List<String> items = new ArrayList<String>();
    for (ElementDefn e : parent.getRoot().getElements()) {
      if (!Utilities.noString(e.getW5()))
        items.add(e.getName()+"(="+e.getW5()+")");
    }
    return items.toString();
  }

  private boolean hasW5Mappings(ResourceDefn parent) {
    for (ElementDefn e : parent.getRoot().getElements()) {
      if (!Utilities.noString(e.getW5()))
        return true;
    }
    return false;
  }

  private String trimIndexes(String p) {
    while (p.contains("("))
      if (p.indexOf(")") == p.length()-1)
        p = p.substring(0, p.indexOf("("));
      else
        p = p.substring(0, p.indexOf("("))+p.substring(p.indexOf(")")+1);
    return p;
  }

  private boolean searchNameOk(String code) {
    String[] ws = code.split("\\-");
    for (String w : ws) {
      if (!speller.ok(w))
        return false;
    }
    return true;
  }

  private boolean hasOpExample(List<OperationExample> examples, boolean resp) {
    for (OperationExample ex: examples) {
      if (ex.isResponse() == resp)
        return true;
    }
    return false;
  }

  private boolean stringMatches(String value, String... args) {
    for(String arg: args) {
      if (value.equalsIgnoreCase(arg))
        return true;      
    }
    return false;
  }

  private boolean hasActivFalse(ResourceDefn parent) {
    ElementDefn e = parent.getRoot().getElementByName(definitions, "active", true, false);
    if (e != null) {
      if (e.typeCode().equals("boolean"))
        return true;
    }
    return false;
  }

  private boolean hasStatus(ResourceDefn parent, String code) {
    ElementDefn e = parent.getRoot().getElementByName(definitions, "status", true, false);
    if (e != null) {
      if (e.hasBinding() && e.getBinding().getValueSet() != null) {
        ValueSet vs = e.getBinding().getValueSet();
        for (ConceptSetComponent inc : vs.getCompose().getInclude()) {
          CodeSystem cs = codeSystems.get(inc.getSystem());
          if (inc.getConcept().isEmpty() && cs != null)
          for (ConceptDefinitionComponent cc : cs.getConcept()) {
            if (cc.getCode().equals(code))
              return true;
          }
        }
      } 
    }
    return false;
  }

  private boolean parentHasOp(String rname, String opname) throws Exception {
    if (Utilities.noString(rname) || "Base".equals(rname))
        return false;
    ResourceDefn r = definitions.getResourceByName(rname);
    for (Operation op : r.getOperations()) {
      if (op.getName().equals(opname))
        return true;
    }    
    return parentHasOp(r.getRoot().typeCode(), opname);
  }

  private boolean resourceIsTechnical(String name) {
    return 
        name.equals("AuditEvent") || 
        name.equals("Binary") || 
        name.equals("Bundle") || 
        name.equals("ConceptMap") || 
        name.equals("CapabilityStatement") || 
        name.equals("MessageHeader") || 
        name.equals("Subscription") || 
        name.equals("ImplementationGuide") ||
        name.equals("DataElement") || 
        name.equals("NamingSystem") || 
        name.equals("SearchParameter") || 
        name.equals("GraphDefinition") || 
        name.equals("Provenance") || 
        name.equals("Query") || 
        name.equals("ValueSet") ||         
        name.equals("OperationDefinition") ||         
        name.equals("OperationOutcome");         
  }


  public List<ValidationMessage> check(String name, ResourceDefn parent) throws Exception {
    List<ValidationMessage> errors = new ArrayList<ValidationMessage>();
    check(errors, name, parent);    
    return errors;
  }
  
	//todo: check that primitives *in datatypes* don't repeat
	
	private int checkElement(List<ValidationMessage> errors, String path, ElementDefn e, ResourceDefn parent, String parentName, boolean needsRimMapping, boolean optionalParent, boolean hasSummary, List<String> vsWarns, boolean parentInSummary, StandardsStatus status) throws Exception {
//	  for (TypeRef t : e.getTypes()) {
//  	  if (!typeCounter.containsKey(t.getName()))
//	      typeCounter.put(t.getName(), 1);
//  	  else
//  	    typeCounter.put(t.getName(), typeCounter.get(t.getName())+1);
//	  }
	  checkForCodeableReferenceCandidates(e, path);
	  e.setPath(path);
	  int vsWarnings = 0;
	  if (!names.containsKey(e.getName()))
	    names.put(e.getName(), 0);
    names.put(e.getName(), names.get(e.getName())+1);
	  
    checkPatterns(e);

    rule(errors, IssueType.STRUCTURE, path, e.getName().length() < maxElementLength, "Name "+e.getName()+" is too long (max element name length = "+Integer.toString(maxElementLength));
    rule(errors, IssueType.STRUCTURE, path, isValidToken(e.getName(), !path.contains(".")), "Name "+e.getName()+" is not a valid element name");
	  rule(errors, IssueType.STRUCTURE, path, e.unbounded() || e.getMaxCardinality() == 1,	"Max Cardinality must be 1 or unbounded");
		rule(errors, IssueType.STRUCTURE, path, e.getMinCardinality() == 0 || e.getMinCardinality() == 1, "Min Cardinality must be 0 or 1");
		rule(errors, IssueType.STRUCTURE, path, !e.getName().equals("div") || e.typeCode().equals("xhtml"), "Any element named 'div' must have a type of 'xhtml'");

		if (e.typeCode().startsWith("Reference"))
		  patternFinder.registerReference(parent.getRoot(), e);
		
		if (status == StandardsStatus.NORMATIVE && e.getStandardsStatus() == null && e.getTypes().size() == 1) {
		  if (definitions.hasElementDefn(e.typeCode())) {
		    TypeDefn t = definitions.getElementDefn(e.typeCode());
		    if (t != null && t.getStandardsStatus() != StandardsStatus.NORMATIVE)
          e.setStandardsStatus(t.getStandardsStatus());
        e.setNormativeVersion(null);
		  }
		}
    if (!hasSummary)
      e.setSummaryItem(true);
    else if (parentInSummary) {
      rule(errors, IssueType.STRUCTURE, path, hasSummary(e) || !e.isModifier(),  "A modifier element must be in the summary ("+path+")");
      rule(errors, IssueType.STRUCTURE, path, hasSummary(e) || e.getMinCardinality() == 0,  "A required element (min > 0) must be in the summary ("+path+")");
    }
      
    rule(errors, IssueType.STRUCTURE, path, optionalParent || e.isSummary() || !path.contains(".") || e.getMinCardinality() == 0,  "An element with a minimum cardinality = 1 must be in the summary ("+path+")");
    optionalParent = optionalParent || e.getMinCardinality() == 0;
    
		hint(errors, IssueType.STRUCTURE, path, !nameOverlaps(e.getName(), parentName), "Name of child ("+e.getName()+") overlaps with name of parent ("+parentName+")");
    checkDefinitions(errors, path, e);
    warning(errors, IssueType.STRUCTURE, path, !path.contains(".") || !Utilities.isPlural(e.getName()) || !e.unbounded(), "Element names should be singular");
    rule(errors, IssueType.STRUCTURE, path, !e.getName().equals("id"), "Element named \"id\" not allowed");
    warning(errors, IssueType.STRUCTURE, path, !e.getName().equals("comments"), "Element named \"comments\" not allowed - use 'comment'");
    warning(errors, IssueType.STRUCTURE, path, !e.getName().equals("notes"), "Element named \"notes\" not allowed - use 'note'");
    rule(errors, IssueType.STRUCTURE, path, !e.getName().endsWith("[x]") || !e.unbounded(), "Elements with a choice of types cannot have a cardinality > 1");
    rule(errors, IssueType.STRUCTURE, path, !e.getName().equals("extension"), "Element named \"extension\" not allowed");
    rule(errors, IssueType.STRUCTURE, path, !e.getName().equals("entries"), "Element named \"entries\" not allowed");
 
    rule(errors, IssueType.STRUCTURE, path, e.getName().equals(path) || e.getElements().size() == 0 || (e.hasSvg() || e.isUmlBreak() || !Utilities.noString(e.getUmlDir())), "Element is missing a UML layout direction");
//Comment out until STU 4
    //    hint(errors, IssueType.BUSINESSRULE, path, !e.isModifier() || e.getMinCardinality() > 0 || e.getDefaultValue()!=null, "if an element is modifier = true, minimum cardinality should be > 0 if no default is specified");
    rule(errors, IssueType.STRUCTURE, path, !e.getDefinition().toLowerCase().startsWith("this is"), "Definition should not start with 'this is'");
    rule(errors, IssueType.STRUCTURE, path, e.getDefinition().endsWith(".") || e.getDefinition().endsWith("?") , "Definition should end with '.' or '?', but is '"+e.getDefinition()+"'");
    if (e.usesType("string") && e.usesType("CodeableConcept"))
      rule(errors, IssueType.STRUCTURE, path, e.hasComments() && e.getComments().contains("string") && e.getComments().contains("CodeableConcept"), "Element type cannot have both string and CodeableConcept unless the difference between their usage is explained in the comments");
    warning(errors, IssueType.BUSINESSRULE, path, Utilities.noString(e.getTodo()), "Element has a todo associated with it ("+e.getTodo()+")");
    
    if (!Utilities.noString(e.getW5())) {
      if (path.contains("."))
        rule(errors, IssueType.INVALID, path, definitions.getW5s().containsKey(e.getW5()), "The w5 value '"+e.getW5()+"' is illegal");
      else {
        String[] vs = e.getW5().split("\\."); 
        rule(errors, IssueType.INVALID, path, vs.length ==2 && definitions.getW5s().containsKey(vs[0]) && definitions.getW5s().get(vs[0]).getSubClasses().contains(vs[1]), "The w5 value '"+e.getW5()+"' is illegal");
      }
    }
    if (e.getName().equals("subject"))
      warning(errors, IssueType.STRUCTURE, path, !e.typeCode().equals("Reference(Patient)"), "Elements with name 'subject' cannot be a reference to just a patient"); // make this an error...
    if (e.getName().equals("patient"))
      rule(errors, IssueType.STRUCTURE, path, e.typeCode().equals("Reference(Patient)"), "Elements with name 'patient' must be a reference to just a patient");
    
//    if (needsRimMapping)
//      suppressedwarning(errors, IssueType.REQUIRED, path, !Utilities.noString(e.getMapping(ElementDefn.RIM_MAPPING)), "RIM Mapping is required");

    if (e.getName().equals("comment")) {
      warning(errors, IssueType.STRUCTURE, path, isOkComment(path), "MnM must have confirmed this should not be an Annotation");
      warning(errors, IssueType.STRUCTURE, path, Utilities.existsInList(e.typeCode(), "string" ,"markdown"), "The type of 'comment' must be 'string' or 'markdown'");
      warning(errors, IssueType.STRUCTURE, path, e.getMinCardinality() == 0, "The min cardinality of 'comment' must be 0");
      warning(errors, IssueType.STRUCTURE, path, e.getMaxCardinality() == 1, "The max cardinality of 'comment' must be 1");
    }
    if (e.getName().equals("note")) {
      warning(errors, IssueType.STRUCTURE, path, e.typeCode().equals("Annotation"), "The type of 'note' must be 'Annotation'");
      warning(errors, IssueType.STRUCTURE, path, e.getMinCardinality() == 0, "The min cardinality of 'note' must be 0");
      warning(errors, IssueType.STRUCTURE, path, e.unbounded(), "The max cardinality of 'note' must be *");
    }
    String sd = e.getShortDefn();

		
    for (String inv : e.getInvariants().keySet()) {
      String xpath = e.getInvariants().get(inv).getXpath();
      rule(errors, IssueType.VALUE, path,  !(xpath.contains("&lt;") || xpath.contains("&gt;")), "error in xpath - do not escape xml characters in the xpath in the excel spreadsheet");
    }
    rule(errors, IssueType.STRUCTURE, path, !e.getName().startsWith("_"), "Element names cannot start with '_'");
		// if (e.getConformance() == ElementDefn.Conformance.Mandatory &&
		// !e.unbounded())
		// rule(errors, path, e.getMinCardinality() > 0,
		// "Min Cardinality cannot be 0 when element is mandatory");
		//TODO: Really? A composite element need not have a definition?
		checkType(errors, path, e, parent);
//		rule(errors, path, !"code".equals(e.typeCode()) || e.hasBinding(),
//				"Must have a binding if type is 'code'");

    rule(errors, IssueType.STRUCTURE, path, !"uuid".equals(e.typeCode()), "The type uuid is illegal");
		if (e.typeCode().equals("code") && parent != null && !e.isNoBindingAllowed()) {
		  rule(errors, IssueType.STRUCTURE, path, e.hasBinding(), "An element of type code must have a binding");
		}
    if ((e.usesType("Coding") && !parentName.equals("CodeableConcept")) || (e.usesType("CodeableConcept") && !(e.usesType("Reference") || e.usesType("Quantity") || e.usesType("SimpleQuantity")))) {
      hint(errors, IssueType.STRUCTURE, path, e.hasBinding(), "An element of type CodeableConcept or Coding must have a binding");
    }
    if (e.getTypes().size() > 1) {
      Set<String> types = new HashSet<String>();
      for (TypeRef t : e.getTypes()) {
        String base = null;
        if (definitions.getConstraints().containsKey(t.getName()))
          base = definitions.getConstraints().get(t.getName()).getBaseType();
        else
          base = t.getName();
        rule(errors, IssueType.STRUCTURE, path, !types.contains(base), "Element type combination includes multiple actual types that are the same");
        types.add(base);
      }
    }
		
		if (e.hasBinding()) {
		  boolean ok = false;
		  for (TypeRef tr : e.getTypes()) {
		    ok = ok || Utilities.existsInList(tr.getName(), "code", "id", "Coding", "CodeableConcept", "uri", "Quantity", "CodeableReference");
		  }
		  rule(errors, IssueType.STRUCTURE, path, ok, "Can only specify bindings for coded data types (not ("+e.typeCode()+")");
		  if (e.getBinding().getValueSet() != null && e.getBinding().getValueSet().getName() == null)
		    throw new Error("unnamed value set on "+e.getBinding().getName());
			BindingSpecification cd = e.getBinding();
			
			if (cd != null) {
			  check(errors, path, cd, sd, e);
			  if (cd.getValueSet() != null) {
			    if (e.getBinding().getStrength() == BindingStrength.EXAMPLE)
	          ValueSetUtilities.markStatus(cd.getValueSet(), parent == null ? "fhir" : parent.getWg().getCode(), StandardsStatus.DRAFT, null, "1", context, null);
			    else if (parent == null)
            ValueSetUtilities.markStatus(cd.getValueSet(), "fhir", StandardsStatus.DRAFT, null, "0", context, null);
          else if (e.getBinding().getStrength() == BindingStrength.PREFERRED)
            ValueSetUtilities.markStatus(cd.getValueSet(), parent.getWg().getCode(), null, null, null, context, null);
          else 
			      ValueSetUtilities.markStatus(cd.getValueSet(), parent.getWg().getCode(), parent.getStatus(), parent.getNormativePackage(), parent.getFmmLevel(), context, parent.getNormativeVersion());
			    if (cd.getMaxValueSet() != null) {
            ValueSetUtilities.markStatus(cd.getMaxValueSet(), parent.getWg().getCode(), parent.getStatus(), parent.getNormativePackage(), parent.getFmmLevel(), context, parent.getNormativeVersion());
			    }
			    Integer w = (Integer) cd.getValueSet().getUserData("warnings");
			    if (w != null && w > 0 && !vsWarns.contains(cd.getValueSet().getId())) {
			      vsWarnings++;
			      vsWarns.add(cd.getValueSet().getId());
			    }
			  }
			}
		}

    String s = e.getMapping(Definitions.RIM_MAPPING);
    hint(errors, IssueType.REQUIRED, path, !needsRimMapping || !Utilities.noString(s), "RIM Mapping is required");

    needsRimMapping = needsRimMapping && !"n/a".equalsIgnoreCase(s) && !Utilities.noString(s);
    
    for (String uri : definitions.getMapTypes().keySet()) {
      MappingSpace m = definitions.getMapTypes().get(uri);
      if (m.isPattern()) {
        String map = e.getMapping(uri);
        if (!Utilities.noString(map)) {
          String err = checkPatternMap(e, map);
          rule(errors, IssueType.STRUCTURE, path, err == null, "Pattern "+m.getTitle()+" is invalid at "+path+": "+err);
        }
      }
    }
    
    // check name uniqueness
    for (ElementDefn c : e.getElements()) {
      String name = c.getName();
      if (name.endsWith("[x]")) {
        name = name.substring(0, name.length()-3);
        for (ElementDefn c2 : e.getElements()) {
          if (c != c2)
            rule(errors, IssueType.STRUCTURE, path, !c2.getName().startsWith(name) || !definitions.hasType(c2.getName().substring(name.length())), "Duplicate Child Name "+c.getName()+"/"+c2.getName()+" at "+path);
        }
      }
    }
    
    for (ElementDefn c : e.getElements()) {
      vsWarnings = vsWarnings + checkElement(errors, path + "." + c.getName(), c, parent, e.getName(), needsRimMapping, optionalParent, hasSummary, vsWarns, parentInSummary && hasSummary(e), status);
    }
		return vsWarnings;
	}

	private void checkForCodeableReferenceCandidates(ElementDefn e, String path) {
    List<ElementDefn> ccs = new ArrayList();
    List<ElementDefn> refs = new ArrayList();

    for (ElementDefn c : e.getElements()) {
      if (c.getMaxCardinality() > 1) {
        for (TypeRef tr : c.getTypes()) {
          if (tr.getName().equals("CodeableConcept")) {
            ccs.add(c);
          }
          if (tr.getName().equals("Reference")) {
            refs.add(c);
          }
        }
      }
    }
    if (ccs.size() > 0 && refs.size() > 0) {
      List<String> m = new ArrayList<String>();
      for (ElementDefn ecc : ccs) {
        for (ElementDefn eref : refs) {
          if (ecc == eref) {
            if (ecc.getTypes().size() == 2) {
              m.add(path+"."+ecc.getName());
            }
          } else if (!"".equals(firstWord(ecc.getName())) && firstWord(ecc.getName()).equals(firstWord(eref.getName()))) {
            m.add(path+"."+firstWord(ecc.getName()));
          }
        }
      }
      for (String s : m) {
        System.out.println("Candidate for CodeableReference: "+m);
      }
    }
  }

  private String firstWord(String words) { 
    int t = -1;
    for (int i = 0; i < words.length(); i++) {
      if (Character.isUpperCase(words.charAt(i)) || words.charAt(i) == '[') {
        t = i;
        break;
      }
    }
    return t < 1 ? "" : words.substring(0, t);
  }

  private int commonLeftHand(String scc, String sref) {
    for (int i = 0; i < Integer.min(scc.length(), sref.length()); i++) {
      if (scc.charAt(i) != sref.charAt(i)) {
        return i;
      }
    }
    return Integer.min(scc.length(), sref.length());
  }

  private boolean startsWithType(String sd) {
    for (String t : definitions.getPrimitives().keySet())
      if (sd.startsWith(t))
        return true;
    return false;
  }

  private String checkPatternMap(ElementDefn ed, String map) {
	  return null;
//	  String[] parts = map.split("\\.");
//    LogicalModel lm = definitions.getLogicalModel(parts[0].toLowerCase());
//    if (lm == null) 
//      return "Unable to find pattern \""+parts[0]+"\"";
//    else {
//      ElementDefn pd = lm.getResource().getRoot().getElementByName(definitions, map, true, true);
//      if (pd == null) 
//        return "Unable to find pattern \""+parts[0]+"\"";
//      else {
//        if (ed.getMinCardinality() < pd.getMinCardinality())
//          return "Cardinality mismatch: element ("+ed.getPath()+") min is "+ed.getMinCardinality()+" but pattern ("+map+") min is "+pd.getMinCardinality();
//        if (ed.getMaxCardinality() > pd.getMaxCardinality())
//          return "Cardinality mismatch: element ("+ed.getPath()+") min is "+ed.getMinCardinality()+" but pattern ("+map+") min is "+pd.getMinCardinality();
//        if (!patternTypeIsCompatible(ed, pd))
//          return "Type mismatch: element  ("+ed.getPath()+")  is "+ed.typeCode()+" but pattern ("+map+")  is "+pd.typeCode();
//      }
//    }
//    
//    return null;
  }

//  private boolean patternTypeIsCompatible(ElementDefn ed, ElementDefn pd) {
//    for (TypeRef et : ed.getTypes())
//      for (TypeRef pt : pd.getTypes())
//        if (patternTypeIsCompatible(et, pt))
//          return true;
//    return false;
//  }
//
//  private boolean patternTypeIsCompatible(TypeRef et, TypeRef pt) {
//    if (et.getName().equals(pt.getName()))
//      return true;
//    if (typesAre(et, pt, "string", "CodeableConcept"))
//      return true;
//    if (typesAre(et, pt, "code", "CodeableConcept"))
//      return true;
//    if (typesAre(et, pt, "boolean", "CodeableConcept"))
//      return true;
//    if (typesAre(et, pt, "string", "Annotation"))
//      return true;
//    if (typesAre(et, pt, "instant", "dateTime"))
//      return true;
//    return false;
//  }
//
//  private boolean typesAre(TypeRef et, TypeRef pt, String t1, String t2) {
//    return et.getName().equals(t1) && pt.getName().equals(t2);
//  }
//
  // MnM controls this list
  private boolean isOkComment(String path) {
    return Utilities.existsInList(path, "Appointment.comment", "AppointmentResponse.comment", "HealthcareService.comment", "Schedule.comment", "Slot.comment", "DiagnosticReport.image.comment");
  }

  private boolean isValidToken(String name, boolean root) {
    if (Utilities.noString(name))
      return false;
    for (char c : name.toCharArray()) {
      if (!isValidChar(c))
        return false;
    }
    if (!Character.isLetter(name.charAt(0)))
      return false;
    if (root && !Character.isUpperCase(name.charAt(0)))
      return false;
    if (!root && !Character.isLowerCase(name.charAt(0)))
      return false;
    return true;
  }

  private boolean isValidChar(char c) {
    if (c >= 'a' && c <= 'z')
      return true;
    if (c >= 'A' && c <= 'Z')
      return true;
    if (c >= '0' && c <= '9')
      return true;
    if (c == '[' || c == ']')
      return true;
    return false;
  }

  private boolean isExemptFromCodeList(String path) {
    return path.equals("Timing.repeat.when");
  }

  private boolean hasGoodCode(List<DefinedCode> codes) {
    for (DefinedCode d : codes) 
      if (!Utilities.isInteger(d.getCode()) && d.getCode().length() > 1)
        return true;
    return false;
  }

  private void checkDefinitions(List<ValidationMessage> errors, String path, ElementDefn e) {
    rule(errors, IssueType.STRUCTURE, path, e.hasDefinition(), "A Definition is required");
    
    if (!e.hasShortDefn()) 
      return;
    
    warning(errors, IssueType.STRUCTURE, path, !e.getShortDefn().equals(e.getDefinition()), "Element needs a definition of its own");
    warning(errors, IssueType.STRUCTURE, path, !e.getShortDefn().equals(e.getName()), "Short description can't be the same as the name");
    Set<String> defn = new HashSet<String>();
    for (String w : splitByCamelCase(e.getName()).toLowerCase().split(" ")) 
      defn.add(Utilities.pluralizeMe(w));
    for (String w : path.split("\\.")) 
      for (String n : splitByCamelCase(w).split(" ")) 
        defn.add(Utilities.pluralizeMe(n.toLowerCase()));
    
    Set<String> provided = new HashSet<String>();
    for (String w : stripPunctuation(splitByCamelCase(e.getShortDefn()), false).split(" "))
      if (!Utilities.noString(w) && !grammarWord(w.toLowerCase()))
        provided.add(Utilities.pluralizeMe(w.toLowerCase()));
    boolean ok = false;
    for (String s : provided)
      if (!defn.contains(s))
        ok = true;
    warning(errors, IssueType.STRUCTURE, path, ok, "Short description doesn't add any new content: '"+e.getShortDefn()+"'");
  }

  private boolean nameOverlaps(String name, String parentName) {
	  if (Utilities.noString(parentName))
	    return false;
	  if (name.equals(parentName))
      return false; // special case
	  String[] names = Utilities.splitByCamelCase(name);
	  String[] parentNames = Utilities.splitByCamelCase(parentName);
	  for (int i = 1; i <= names.length; i++) {
	    if (arraysMatch(copyLeft(names, i), copyRight(parentNames, i)))
	      return true;
	  }
	  return false;
  }

  private boolean arraysMatch(String[] a1, String[] a2) {
    if (a1.length != a2.length)
      return false;
    for (int i = 0; i < a1.length; i++) 
      if (!a1[i].equals(a2[i]))
           return false;
    return true;
  }

  private String[] copyLeft(String[] names, int length) {
    String[] p = new String[Math.min(length, names.length)];
    for (int i = 0; i < p.length; i++)
      p[i] = names[i];
    return p;
  }
  
  private String[] copyRight(String[] names, int length) {
    String[] p = new String[Math.min(length, names.length)];
    for (int i = 0; i < p.length; i++)
      p[i] = names[i + Math.max(0, names.length - length)];
    return p;
  }

  private void checkType(List<ValidationMessage> errors, String path, ElementDefn e, ResourceDefn parent) {
    if (e.getTypes().size() == 0) {
      rule(errors, IssueType.STRUCTURE, path, path.contains("."), "Must have a type on a base element");
      rule(errors, IssueType.STRUCTURE, path, e.getName().equals("extension") || e.getElements().size() > 0, "Must have a type unless sub-elements exist");
    } else {
      rule(errors, IssueType.STRUCTURE, path, e.getTypes().size() == 1 || e.getName().endsWith("[x]"), "If an element has a choice of data types, its name must end with [x]");
      if (definitions.dataTypeIsSharedInfo(e.typeCode())) {
        try {
          e.getElements().addAll(definitions.getElementDefn(e.typeCode()).getElements());
        } catch (Exception e1) {
          rule(errors, IssueType.STRUCTURE, path, false, e1.getMessage());
        }
      } else {
        for (TypeRef t : e.getTypes()) 
        {
          String s = t.getName();
          if (s.charAt(0) == '@') {
            //TODO: validate path
          } 
          else 
          {
            if (s.charAt(0) == '#')
              s = s.substring(1);
            if (!t.isSpecialType()) {
              rule(errors, IssueType.STRUCTURE, path, typeExists(s, parent), "Illegal Type '" + s + "'");
              if (t.isResourceReference()) {
                for (String p : t.getParams()) {
                  rule(errors, IssueType.STRUCTURE, path,
                      p.equals("Any")
                      || definitions.hasResource(p),
                      "Unknown resource type " + p);
                }
              }
            }
          }
        }
      }
    }
	}

	private boolean typeExists(String name, ResourceDefn parent) {
		return definitions.hasType(name) || definitions.getBaseResources().containsKey(name) ||
				(parent != null && parent.getRoot().hasNestedType(name));
	}

	
//	private List<ValidationMessage> check(String n, BindingSpecification cd) throws Exception {
//    List<ValidationMessage> errors = new ArrayList<ValidationMessage>();
//    check(errors, n, cd);
//    return errors;
//  }
  
	private void check(List<ValidationMessage> errors, String path, BindingSpecification cd, String sd, ElementDefn e) throws Exception  {
    // basic integrity checks
    List<DefinedCode> ac = cd.getAllCodes(definitions.getCodeSystems(), definitions.getValuesets(), false);
    for (DefinedCode c : ac) {
      String d = c.getCode();
      if (Utilities.noString(d))
        d = c.getId();
      if (Utilities.noString(d))
        d = c.getDisplay();
      if (Utilities.noString(d))
        d = c.getDisplay();
      
      if (Utilities.noString(c.getSystem()))
        warning(errors, IssueType.STRUCTURE, "Binding @ "+path, !Utilities.noString(c.getDefinition()), "Code "+d+" must have a definition");
      rule(errors, IssueType.STRUCTURE, "Binding @ "+path, !(Utilities.noString(c.getId()) && Utilities.noString(c.getSystem())) , "Code "+d+" must have a id or a system");
    }
    
    // trigger processing into a Heirachical set if necessary
//    rule(errors, IssueType.STRUCTURE, "Binding @ "+path, !cd.isHeirachical() || (cd.getChildCodes().size() < cd.getCodes().size()), "Logic error processing Hirachical code set");

    // now, rules for the source
    hint(errors, IssueType.STRUCTURE, "Binding @ "+path, cd.getBinding() != BindingMethod.Unbound, "Need to provide a binding");
    rule(errors, IssueType.STRUCTURE, "Binding @ "+path, Utilities.noString(cd.getDefinition())  || (cd.getDefinition().charAt(0) == cd.getDefinition().toUpperCase().charAt(0)), "Definition cannot start with a lowercase letter");
    if (cd.getBinding() == BindingMethod.CodeList || (cd.getBinding() == BindingMethod.ValueSet && cd.getStrength() == BindingStrength.REQUIRED && ac.size() > 0 && "code".equals(e.typeCode()))) {
      if (path.toLowerCase().endsWith("status")) {
        if (rule(errors, IssueType.STRUCTURE, path, definitions.getStatusCodes().containsKey(path), "Status element not registered in status-codes.xml")) {
//          rule(errors, IssueType.STRUCTURE, path, e.isModifier(), "Status elements that map to status-codes should be labelled as a modifier");
          for (DefinedCode c : ac) {
            boolean ok = false;
            for (String s : definitions.getStatusCodes().get(path)) {
              String[] parts = s.split("\\,");
              for (String p : parts)
                if (p.trim().equals(c.getCode()))
                  ok = true;
            }
            rule(errors, IssueType.STRUCTURE, path, ok, "Status element code \""+c.getCode()+"\" not found in status-codes.xml");
          }
          for (String s : definitions.getStatusCodes().get(path)) {
            String[] parts = s.split("\\,");
            for (String p : parts) {
              if (!Utilities.noString(p)) {
                boolean ok = false;
                for (DefinedCode c : ac) {
                  if (p.trim().equals(c.getCode()))
                    ok = true;
                }
                if (!ok)
                  rule(errors, IssueType.STRUCTURE, path, ok, "Status element code \""+p+"\" found in status-codes.xml but has no matching code");
              }            
            }
          }
        }
      }
      StringBuilder b = new StringBuilder();
      for (DefinedCode c : ac) {
      	if (!c.getAbstract())
        	b.append(" | ").append(c.getCode());
      }
      if (sd.equals("*")) {
        e.setShortDefn(b.toString().substring(3));
        sd = b.toString().substring(3);
      }
        
      if (sd.contains("|")) {
        if (b.length() < 3)
          throw new Error("surprise");
        String esd = b.substring(3);
        rule(errors, IssueType.STRUCTURE, path, sd.startsWith(esd) || (sd.endsWith("+") && b.substring(3).startsWith(sd.substring(0, sd.length()-1)) ), "The short description \""+sd+"\" does not match the expected (\""+b.substring(3)+"\")");
      } else {
        rule(errors, IssueType.STRUCTURE, path, cd.getStrength() != BindingStrength.REQUIRED || ac.size() > 12 || ac.size() <= 1 || !hasGoodCode(ac) || isExemptFromCodeList(path), 
            "The short description of an element with a code list should have the format code | code | etc (is "+sd.toString()+") ("+ac.size()+" codes = \""+b.toString()+"\")");
      }
    }
    boolean isComplex = !e.typeCode().equals("code");

    if (isComplex && cd.getValueSet() != null && hasInternalReference(cd.getValueSet()) && cd.getStrength() != BindingStrength.EXAMPLE) {
      hint(errors, IssueType.BUSINESSRULE, path, false, "The value "+cd.getValueSet().getUrl()+" defines codes, but is used by a Coding/CodeableConcept @ "+path+", so it should not use FHIR defined codes");
      cd.getValueSet().setUserData("vs-val-warned", true);
    }
    
    if (cd.getElementType() == ElementType.Unknown) {
      if (isComplex)
        cd.setElementType(ElementType.Complex);
      else
        cd.setElementType(ElementType.Simple);
    } else if (isComplex && !cd.hasMax())
      rule(errors, IssueType.STRUCTURE, path, cd.getElementType() == ElementType.Complex, "Cannot use a binding from both code and Coding/CodeableConcept elements");
    else
      rule(errors, IssueType.STRUCTURE, path, cd.getElementType() == ElementType.Simple, "Cannot use a binding from both code and Coding/CodeableConcept elements");
    if (isComplex && cd.getValueSet() != null) {
      for (ConceptSetComponent inc : cd.getValueSet().getCompose().getInclude()) 
        if (inc.hasSystem())
          txurls.add(inc.getSystem());
    }
    rule(errors, IssueType.STRUCTURE, "Binding @ "+path, (cd.getElementType() != ElementType.Simple || cd.getStrength() == BindingStrength.REQUIRED || cd.hasMax()) || isExemptFromProperBindingRules(path), "Must be a required binding if bound to a code instead of a Coding/CodeableConcept");
    rule(errors, IssueType.STRUCTURE, "Binding @ "+path, cd.getElementType() != ElementType.Simple || cd.getBinding() != BindingMethod.Unbound, "Need to provide a binding for code elements");
    if (!isComplex && !externalException(path)) {
      ValueSet vs = cd.getValueSet();
      if (warning(errors, IssueType.REQUIRED, path, vs != null || cd.hasReference(), "Unable to resolve value set on 'code' Binding")) {
        hint(errors, IssueType.REQUIRED, path, noExternals(vs), "Bindings for code data types should only use internally defined codes ("+vs.getUrl()+")");
        // don't disable this without discussion on Zulip
      }
    }
    
  }
    
  

	private void checkPatterns(ElementDefn e) {
	  for (TypeRef tr : e.getTypes()) {
	    List<String> types = new ArrayList<>();
	    for (String p : tr.getParams()) {
	      if (definitions.hasLogicalModel(p)) 
	        types.addAll(definitions.getLogicalModel(p).getImplementations());
	      else
	        types.add(p);
	    }

	    if (types.size() > 1) {
	      List<String> patterns = new ArrayList<>();
	      for (ImplementationGuideDefn ig : definitions.getIgs().values()) {
	        for (LogicalModel lm : ig.getLogicalModels()) {
	          patterns.add(lm.getResource().getRoot().getName());
	        }
	      }

	      for (String t : types) {
	        List<String> remove = new ArrayList<>();
	        for (String n : patterns) {
	          if (!definitions.getLogicalModel(n).getImplementations().contains(t))
	            remove.add(n);
	        }
	        patterns.removeAll(remove);
	      }
	      tr.setPatterns(patterns);
	    }
	  }
	}

  private boolean externalException(String path) {
    return Utilities.existsInList(path, "Attachment.language", "Binary.contentType", "Composition.confidentiality");
}

  private boolean noExternals(ValueSet vs) {
    if (Utilities.existsInList(vs.getUrl(), "http://hl7.org/fhir/ValueSet/mimetypes", "http://hl7.org/fhir/ValueSet/languages"))
      return true;
    
    for (ConceptSetComponent inc : vs.getCompose().getInclude()) {
      if (inc.hasValueSet())
        throw new Error("not handled yet");
      if (inc.getSystem().startsWith("http://terminology.hl7.org/CodeSystem/v2-") || inc.getSystem().startsWith("http://terminology.hl7.org/CodeSystem/v3-"))
        return false;
      if (!Utilities.existsInList(inc.getSystem(), "urn:iso:std:iso:4217", "http://unitsofmeasure.org") && !inc.getSystem().startsWith("http://hl7.org/fhir/"))
        return false;
    }
    return true;
  }

  // grand fathered in, to be removed
	private boolean isExemptFromProperBindingRules(String path) {
    return Utilities.existsInList(path, "ModuleMetadata.type", 
        "ActionDefinition.type", "ElementDefinition.type.code", "Account.status", "MedicationOrder.category", "MedicationStatement.category", "Sequence.type", 
        "StructureDefinition.type", "ImplementationGuide.definition.parameter.code", "TriggerDefinition.condition.language");
  }

  private boolean hasInternalReference(ValueSet vs) {
	  for (ConceptSetComponent inc : vs.getCompose().getInclude()) {
	    String url = inc.getSystem();
	    if (!Utilities.noString(url) && url.startsWith("http://hl7.org/fhir") && !url.contains("/v2/") && !url.contains("/v3/"))
	      return false;
	  }
	  return false;
	}

  public void dumpParams() {
//    for (String s : usages.keySet()) {
//      System.out.println(s+": "+usages.get(s).usage.toString());
//    }
//    for (SearchType s : usagest.keySet()) {
//      System.out.println(s.toString()+": "+usagest.get(s).usage.toString());
//    }
  }

  public void report() {
    // for dumping of ad-hoc summaries from the checking phase
//    for (String t : typeCounter.keySet()) {
//      System.out.println(t+": "+typeCounter.get(t).toString());
//    }
    // for tracking individual name usage
    
//    int total = 0;
//    for (String n : names.keySet()) {
//      System.out.println(n+" = "+names.get(n));
//      total += names.get(n);
//    }
//    System.out.println("total = "+Integer.toString(total));
//    for (String s : txurls) {
//      if (!s.startsWith("http://terminology.hl7.org") &&s.startsWith("http://hl7.org/fhir"))
//        System.out.println("URL to fix: "+s);
//    }
  }

  public void summariseSearchTypes(Set<String> searchTypeUsage) {
    for (SearchType st : usagest.keySet()) {
      for (String u : usagest.get(st).usage) {
        searchTypeUsage.add(u+":"+st.name());
      }
    }
    
  }

  public void close() throws Exception {
    speller.close();
  }

  public String searchParamGroups() {
    StringBuilder b = new StringBuilder();
    for (SearchParameterGroup spg : spgroups.values()) {
      if (spg.resources.size() > 1) {
        b.append(spg.name);
        b.append(" : ");
        b.append(spg.type);
        b.append(" = ");
        b.append(spg.resources.toString());
        b.append("\r\n");
      }
    }
    
    return b.toString();
  }

  public List<ValidationMessage> check(Compartment cmp) {
    List<ValidationMessage> errors = new ArrayList<ValidationMessage>();
    for (ResourceDefn rd : cmp.getResources().keySet()) {
      String[] links = cmp.getResources().get(rd).split("\\|");
      for (String l : links) {
        String s = l.trim();
        if (!Utilities.noString(s) && !s.equals("{def}")) {
          SearchParameterDefn spd = rd.getSearchParams().get(s);
          if (rule(errors, IssueType.STRUCTURE, "compartment."+cmp.getName()+"."+rd.getName()+"."+s, spd != null, "Search Parameter '"+s+"' not found")) { 
            if (rule(errors, IssueType.STRUCTURE, "compartment."+cmp.getName()+"."+rd.getName()+"."+s, spd.getType() == SearchType.reference, "Search Parameter '"+s+"' not a reference")) {
              boolean ok = false;
              for (String p : spd.getPaths()) {
                ElementDefn ed;
                try {
                  ed = definitions.getElementByPath(p.split("\\."), "matching compartment", true);
                } catch (Exception e) {
                  rule(errors, IssueType.STRUCTURE, "compartment."+cmp.getName()+"."+rd.getName()+"."+s, ok, "Illegal path "+p);
                  ed = null;
                }
                if (ed != null) {
                  for (TypeRef tr : ed.getTypes()) {
                    for (String tp : tr.getParams()) {
                      if (definitions.hasLogicalModel(tp)) {
                        ok = ok || definitions.getLogicalModel(tp).getImplementations().contains(cmp.getTitle());
                      } else
                        ok = ok || tp.equals(cmp.getTitle()) || tp.equals("Any");
                    }
                  }
                }
              }
              rule(errors, IssueType.STRUCTURE, "compartment."+cmp.getName()+"."+rd.getName()+"."+s, ok, "No target match for "+cmp.getTitle());
            }
          }
        }
      }
    }
    return errors;
  }

  public void resolvePatterns() throws FHIRException {
    List<LogicalModel> list = new ArrayList<>();
    
    for (ImplementationGuideDefn ig : definitions.getSortedIgs()) {
      for (LogicalModel lm : ig.getLogicalModels()) {
        list.add(lm);
      }
    }
    
    for (LogicalModel lm : list) {
      String name = lm.getResource().getRoot().getName();
      List<String> names = definitions.listAllPatterns(name);
      for (String rn : definitions.sortedResourceNames()) {
        ResourceDefn r = definitions.getResourceByName(rn);
        if (names.contains(r.getRoot().getMapping(lm.getMappingUrl()))) {
          lm.getImplementations().add(rn);
        }            
      }
    }
    for (ResourceDefn rd : definitions.getResources().values()) {
      for (SearchParameterDefn sp : rd.getSearchParams().values()) {
        for (LogicalModel lm : list) {
          if (sp.getTargets().contains(lm.getResource().getRoot().getName())) {
            sp.getTargets().remove(lm.getResource().getRoot().getName());
            sp.getTargets().addAll(lm.getImplementations());            
          }
        }        
      }
    }
  }

  public PatternFinder getPatternFinder() {
    return patternFinder;
  }
  
  
}
