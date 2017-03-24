package edu.pitt.dbmi.nlp.noble.mentions.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.coder.model.Modifier;
import edu.pitt.dbmi.nlp.noble.ontology.*;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

/**
 * a domain instance is a wrapper for any instance and mention found in text 
 * @author tseytlin
 *
 */
public class Instance {
	protected DomainOntology domainOntology;
	protected Mention mention;
	protected Modifier modifier;
	protected IClass cls;
	protected IInstance instance;
	protected Set<Modifier> modifiers;
	protected Map<String,Set<Instance>> modifierInstances;
	protected Set<Annotation> annotations;
	protected List<Mention> compoundComponents;
	
	
	/**
	 * initilize an instance
	 * @param ontology of the domain
	 * @param m - mention object
	 */
	
	public Instance(DomainOntology ontology,Mention m){
		setDomainOntology(ontology);
		setMention(m);
	}
	
	/**
	 * initilize an instance
	 * @param ontology object
	 * @param m - modifier object
	 */
	
	public Instance(DomainOntology ontology,Modifier m){
		setDomainOntology(ontology);
		setModifier(m);
	}
	
	/**
	 * initilize an instance
	 * @param ontology - domain ontology
	 * @param m  - mention object
	 * @param inst - ontology instance
	 */
	
	public Instance(DomainOntology ontology,Mention m, IInstance inst){
		setDomainOntology(ontology);
		setMention(m);
		instance = inst;
	}
	
	
	public DomainOntology getDomainOntology() {
		return domainOntology;
	}

	public void setDomainOntology(DomainOntology domainOntology) {
		this.domainOntology = domainOntology;
	}

	/**
	 * set mention associated with this instnace
	 * @param mention object
	 */
	public void setMention(Mention mention) {
		this.mention = mention;
		cls = domainOntology.getConceptClass(mention);
		if(mention != null)
			getModifiers().addAll(mention.getModifiers());
		reset();
	}

	/**
	 * get modifier associated with this instance
	 * @return modifier object
	 */
	
	public Modifier getModifier() {
		return modifier;
	}
	
	/**
	 * get mentions that might make up this instance
	 * @return list of component mentions
	 */
	public List<Mention> getCompoundComponents() {
		if(compoundComponents == null)
			compoundComponents = new ArrayList<Mention>();
		return compoundComponents;
	}

	/**
	 * set compound component mentions
	 * @param compoundComponents - list of components
	 */
	public void setCompoundComponents(List<Mention> compoundComponents) {
		this.compoundComponents = compoundComponents;
	}

	/**
	 * set modifier object
	 * @param modifier object 
	 */
	public void setModifier(Modifier modifier) {
		this.modifier = modifier;
		setMention(modifier.getMention());
		if(mention == null)
			cls = domainOntology.getOntology().getClass(modifier.getValue());
		reset();
	}
	
	/**
	 * reset instance information
	 */
	protected void reset(){
		instance = null;
		modifierInstances = null;
		annotations = null;
	}
	
	/**
	 * get mention associated with this class
	 * @return mention object
	 */

	public Mention getMention() {
		return mention;
	}
	
	/**
	 * get a list of mentions associated with this anchor
	 * @return
	 *
	public List<Mention> getMentions(){
		if(mentions == null){
			mentions = new ArrayList<Mention>();
		}
		return mentions;
	}
	 */
	
	/**
	 * get concept class representing this mention
	 * @return class that represents this instance
	 */
	
	public IClass getConceptClass() {
		return cls;
	}
	
	/**
	 * get an instance representing this mention
	 * @return ontology instance
	 */
	public IInstance getInstance() {
		// what's the point if we have no class?
		if(cls == null)
			return null;
		
		// init instance
		if(instance == null){
			// check if we have an actual mention or some generic default value w/out a mention
			if(mention != null){
				
				// if instance is DocumentSection, just make a default one
				if(domainOntology.isTypeOf(cls,DomainOntology.DOCUMENT_SECTION)){
					instance = domainOntology.getDefaultInstance(cls);
				}else{
					instance = domainOntology.createInstance(cls);
				}
				
				// if instance is modifier, but not linguistic modifier (see if we neet to set some other properties
				if(domainOntology.isTypeOf(cls,DomainOntology.MODIFIER) && !domainOntology.isTypeOf(cls,DomainOntology.LINGUISTIC_MODIFER)){
					// instantiate available modifiers
					List<Instance> modifierInstances = createModifierInstanceList();
					
					// go over all restrictions
					for(IRestriction r: domainOntology.getRestrictions(cls)){
						IProperty prop = r.getProperty();
						for(Instance modifierInstance: modifierInstances){
							IInstance modInstance = modifierInstance.getInstance();
							if(modInstance != null && domainOntology.isPropertyRangeSatisfied(prop,modInstance)){
								addModifierInstance(prop.getName(),modifierInstance);
							}else if(modifierInstance.getModifier() != null && prop.getName().equals(modifierInstance.getModifier().getType())){
								// check if this is a number
								IProperty p = cls.getOntology().getProperty(modifierInstance.getModifier().getType());
								if(p != null && p.hasSuperProperty(cls.getOntology().getProperty(DomainOntology.HAS_VALUE))){
									addModifierInstance(prop.getName(),modifierInstance);
								}
							}
						}
					}
				}
				
				// now just add span
				instance.setPropertyValue(cls.getOntology().getProperty(DomainOntology.HAS_SPAN),getInstanceSpan());
				
				
			}else if(modifier != null){
				instance = domainOntology.getOntology().getInstance(cls.getName()+"_default");
				if(instance == null)
					instance = cls.createInstance(cls.getName()+"_default");
				
			}
		}
		return instance;
	}
	
	/**
	 * create a string representation of instance span
	 * @return
	 */
	protected String getInstanceSpan() {
		StringBuilder str = new StringBuilder();
		for(Annotation a: getAnnotations()){
			str.append(a.getStartPosition()+":"+a.getEndPosition()+" ");
		}
		return str.toString().trim();
	}

	/**
	 * get name of this instance (derived from class name)
	 * @return name of this instance
	 */
	public String getName(){
		if(getConceptClass() != null)
			return getConceptClass().getName();
		return modifier != null?modifier.getValue():"unknown";
	}
	
	/**
	 * get human preferred label for this instance
	 * @return label of this instance, returns name if label not available
	 */
	public String getLabel(){
		if(getConceptClass() != null)
			return getConceptClass().getLabel();
		return modifier != null?modifier.getValue():"unknown";
	}
	
	/**
	 * pretty print this name
	 * @return pretty printed version of instance
	 */
	public String toString(){
		StringBuilder str = new StringBuilder();
		str.append(getLabel());
		for(String type: getModifierInstances().keySet()){
			for(Instance modifier:getModifierInstances().get(type)){
				str.append(" "+type+": "+modifier);
			}
		}
		return str.toString();
	}
	
	
	/**
	 * get a mapping of linguistic context found for this mention.
	 *
	 * @return the modifiers map
	 */
	public Map<String,Set<Instance>> getModifierInstances(){
		if(modifierInstances == null){
			modifierInstances = new LinkedHashMap<String,Set<Instance>>();
		}
		return modifierInstances;
	}
	
	/**
	 * get a set of instances associated via given property
	 * @param prop - property by which instances are mapped
	 * @return set of instances
	 */
	public Set<Instance> getModifierInstances(String prop){
		return getModifierInstances().get(prop);
	}
	
	
	/**
	 * get a list of modifiers associated with this instance
	 * @return the modifiers
	 */
	public Set<Modifier> getModifiers(){
		if(modifiers == null){
			modifiers = new LinkedHashSet<Modifier>();
		}
		return modifiers;
	}
	
	/**
	 * get a list of current modifiers as instance list
	 * @return list of modifier instances
	 */
	protected List<Instance> createModifierInstanceList(){
		// instantiate available modifiers as instances
		List<Instance> modifierInstances = new ArrayList<Instance>();
		for(Modifier m: getModifiers()){
			modifierInstances.add(new Instance(domainOntology, m));
		}
		return modifierInstances;
	}

	/**
	 * get a list of current modifiers as instance list
	 * @return list of modifier instances
	 */
	public List<Instance> getModifierInstanceList(){
		if(getModifierInstances().isEmpty())
            return createModifierInstanceList();

        // instantiate available modifiers
		List<Instance> modifierInstances = new ArrayList<Instance>();
		for(String key: getModifierInstances().keySet()){
			modifierInstances.addAll(getModifierInstances(key));
		}
		return modifierInstances;
	}

	/**
	 * add linguistic modifier of this mention.
	 *
	 * @param m the m
	 */
	public void addModifier(Modifier m) {
		getModifiers().add(m);
		reset();
	}
	
	/**
	 * remove a modifier of this mention.
	 *
	 * @param m the m
	 */
	public void removeModifier(Modifier m) {
		getModifiers().remove(m);
		reset();
	}
	
	/**
	 * does this variable has a modifier of a given type?
	 * @param type - type of modifier
	 * @return true or false
	 */
	
	public boolean hasModifierType(String type){
		for(Modifier m: getModifiers()){
			if(m.getType().equals(type))
				return true;
		}
		return false;
	}
	
	/**
	 * get the first modifier of a given type
	 * @param type - type of modifier 
	 */
	public Modifier getModifier(String type){
		for(Modifier m: getModifiers()){
			if(m.getType().equals(type))
				return m;
		}
		return null;
	}
	
	
	/**
	 * add modifier instance
	 * @param property by which modifier is related
	 * @param inst - instance of modifier
	 */
	public void addModifierInstance(String property, Instance inst){
		// add it to the instance
        IOntology ont = domainOntology.getOntology();
		IProperty prop = ont.getProperty(property);
        IClass number = ont.getClass(DomainOntology.NUMERIC_MODIFER);

        // check if this number instance is too general for THIS instance
        IClass vc = inst.getConceptClass();
        if(vc != null && vc.hasSuperClass(number) && !isSatisfied(getConceptClass(),prop,vc)) {
            return;
        }

        // check that what is in the map doesn't already have the same thing
        for(String p: new ArrayList<String>(getModifierInstances().keySet())){
            IProperty pp =  ont.getProperty(p);
            // if the property that is already in the map is a super property?
            if(prop.hasSuperProperty(pp)){
                getModifierInstances().remove(p);
            // if the new property is more general, then don't add it
            }else if(prop.hasSubProperty(pp)){
                return;
            }
        }


        // add property
		if(prop != null && instance != null){
			if(inst.getInstance() != null){
				instance.addPropertyValue(prop, inst.getInstance());
			}else if(prop.isDatatypeProperty() && inst.getModifier() != null){
				instance.addPropertyValue(prop,new Double(TextTools.parseDecimalValue(inst.getLabel())));
			}
		}

		// add to a map
        Set<Instance> list = getModifierInstances().get(property);
        if(list == null){
            list = new LinkedHashSet<Instance>();
        }
        list.add(inst);
        getModifierInstances().put(property,list);


    }

    private boolean isSatisfied(IClass cls, IProperty prop, IClass value){
        // we only care about the first restriction
        for(IRestriction r: cls.getRestrictions(prop)){
            return r.getParameter().evaluate(value);
        }
        return false;
    }



	/**
	 * get a set of text annotations associated with this instance
	 * @return set of annotations
	 */
	public Set<Annotation> getAnnotations() {
		if(annotations == null){
			annotations = new TreeSet<Annotation>();
			if(getMention() != null){
				annotations.addAll(getMention().getAnnotations());
				annotations.addAll(getMention().getModifierAnnotations());
			}
			for(String type: getModifierInstances().keySet()){
				for(Instance modifier:getModifierInstances().get(type)){
					annotations.addAll(modifier.getAnnotations());
				}
			}
		}
		
		return annotations;
	}

	public int hashCode() {
		if(mention != null)
			return mention.hashCode();
		if(modifier != null)
			return modifier.hashCode();
		return super.hashCode();
	}

	public boolean equals(Instance m) {
		if(mention != null && m.getMention() != null)
			return mention.equals(m.getMention());
		else if(modifier != null && m.getModifier() != null)
			return modifier.equals(m.getModifier());
		return super.equals(m);
	}
	
	public String getDefinedConstraints(){
		IClass cls = getConceptClass();
		StringBuilder str = new StringBuilder();
		str.append(cls.getLabel()+"\n");
		for(Object o: cls.getEquivalentRestrictions()){
			str.append("\t"+o+"\n");
		}
		return str.toString();
	}
	
	/**
	 * go over numeric quantities and see if any can be upgraded
	 */
	protected void upgradeNumericModifiers(){
		Set<Modifier> newVals = new HashSet<Modifier>();
		
		for(Modifier mm:  getModifiers()){
			if(mm.hasMention()){
				Mention m = mm.getMention();
				IClass modifierCls = domainOntology.getConceptClass(m);
				if(domainOntology.isTypeOf(modifierCls,DomainOntology.NUMERIC_MODIFER)){
					IProperty hasNumValue = domainOntology.getRelatedProperty(getConceptClass(),mm);
					if(hasNumValue == null)
						continue;
					
					// now go over potential specific instances
					for(IInstance inst: domainOntology.getSpecificInstances(modifierCls)){
						
						// don't bother looking into an instance if it doesn't satisfy the property of this instnace 
						if(!isSatisfied(getConceptClass(), hasNumValue, inst.getDirectTypes()[0]))
							continue;
						
						// clear values
						inst.removePropertyValues();
					
						
						// set data properties
						for(String prop: m.getModifierMap().keySet()){
							IProperty property = domainOntology.getProperty(prop);
							if(property == null)
								continue;
							// add all values
							for(Modifier mod : m.getModifiers(prop)) {
								if (mod.getMention() != null) {
									inst.addPropertyValue(property,domainOntology.getConceptInstance(mod.getMention()));
								} else {
									inst.addPropertyValue(property, new Double(mod.getValue()));
								}
							}
						}
						
						// now check the equivalence
						IClass parentCls = inst.getDirectTypes()[0];
						
						// if instance valid, we found a more specific numeric class
						if(parentCls.getEquivalentRestrictions().evaluate(inst)){
							Mention specificM = domainOntology.getModifierFromClass(parentCls,m);
							Modifier mod = Modifier.getModifier(hasNumValue.getName(),parentCls.getName(),specificM);
							newVals.add(mod);
						}
					}
				}
			}
		}
		getModifiers().addAll(newVals);
	}
	
}
