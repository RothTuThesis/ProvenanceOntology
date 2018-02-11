package ontology;

import java.io.File;
import java.util.Collections;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

public class ProvOntologyMaintainer {

	private String ontologyFileName;
	private OWLReasoner reasoner;
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	private OWLDataFactory factory;
	private OWLReasonerFactory reasonerFactory;
	private OWLReasonerConfiguration config;
	private DefaultPrefixManager pm;
	private OWLClass agentClass, entityClass, organizationClass, activityClass, collectionClass, usageClass;

	private IRI ontologyIRI;
	private IRI provIRI = IRI.create("http://www.w3.org/ns/prov-o#");
	private OWLDataProperty givenNameDataProperty, mboxDataProperty, startedAtTimeDataProperty, endedAtTimeDataProperty,
			sha1DataProperty, locationDataProperty, generatedAtTimeDataProperty, atTimeDataProperty;
	private OWLObjectProperty actedOnBehalfOfObjectProperty, wasGeneratedByObjectProperty,
			wasAssociatedWithObjectProperty, wasDerivedFromObjectProperty, wasAttributedToObjectProperty,
			usedObjectProperty, generatedObjectProperty, hadMemberObjectProperty, wasStartedByObjectProperty,
			qualifiedUsageObjectProperty, qualifiedGenerationObjectProperty;
	private OWLAnnotationProperty usedAtTimeAnnotationProperty;

	public ProvOntologyMaintainer(String ontologyName, String inputOntologyName, String outputOntologyName) {
		try {
			
			ontologyFileName = outputOntologyName;
			ontologyIRI = IRI.create(ontologyName);
			manager = OWLManager.createOWLOntologyManager();
			
			if (inputOntologyName == null)
			{
				ontology = manager.createOntology(ontologyIRI);
			} else {
				ontology = manager.loadOntologyFromOntologyDocument(new File(inputOntologyName));
			}
			
			factory = manager.getOWLDataFactory();
			reasonerFactory = new StructuralReasonerFactory();
			config = new SimpleConfiguration();
			reasoner = reasonerFactory.createReasoner(ontology, config);

			pm = new DefaultPrefixManager();
			pm.setDefaultPrefix(ontologyIRI + "#");
			pm.setPrefix("prov:", provIRI.toString());

			agentClass = factory.getOWLClass("prov:Agent", pm);
			entityClass = factory.getOWLClass("prov:Entity", pm);
			organizationClass = factory.getOWLClass("prov:Organization", pm);
			activityClass = factory.getOWLClass("prov:Activity", pm);
			collectionClass = factory.getOWLClass("prov:Collection", pm);
			usageClass = factory.getOWLClass("prov:Usage", pm);

			startedAtTimeDataProperty = factory.getOWLDataProperty("prov:startedAtTime", pm);
			endedAtTimeDataProperty = factory.getOWLDataProperty("prov:endedAtTime", pm);
			atTimeDataProperty = factory.getOWLDataProperty("prov:atTime", pm);
			generatedAtTimeDataProperty = factory.getOWLDataProperty("prov:generatedAtTime", pm);
			locationDataProperty = factory.getOWLDataProperty("prov:location", pm);

			actedOnBehalfOfObjectProperty = factory.getOWLObjectProperty("prov:actedOnBehalfOf", pm);
			wasGeneratedByObjectProperty = factory.getOWLObjectProperty("prov:wasGeneratedBy", pm);
			generatedObjectProperty = factory.getOWLObjectProperty("prov:generated", pm);
			wasAssociatedWithObjectProperty = factory.getOWLObjectProperty("prov:wasAssociatedWith", pm);
			wasDerivedFromObjectProperty = factory.getOWLObjectProperty("prov:wasDerivedFrom", pm);
			wasAttributedToObjectProperty = factory.getOWLObjectProperty("prov:wasAttributedTo", pm);
			wasStartedByObjectProperty = factory.getOWLObjectProperty("prov:wasStartedBy", pm);
			usedObjectProperty = factory.getOWLObjectProperty("prov:used", pm);
			hadMemberObjectProperty = factory.getOWLObjectProperty("prov:hadMember", pm);
			qualifiedUsageObjectProperty = factory.getOWLObjectProperty("prov:qualifiedUsage", pm);
			qualifiedGenerationObjectProperty = factory.getOWLObjectProperty("prov:qualifiedGeneration", pm);

			usedAtTimeAnnotationProperty = factory.getOWLAnnotationProperty("prov:atTime", pm);

			OWLImportsDeclaration provImport = factory.getOWLImportsDeclaration(provIRI);
			//manager.applyChange(new AddImport(ontology, provImport));
		} catch (OWLOntologyCreationException e) {
			System.out.println("ERROR: " + e.getMessage());
		}
	}

	public OWLOntology getOntology() {
		return ontology;
	}

	public OWLOntologyManager getOntologyManager() {
		return manager;
	}

	public void saveOntology() {
		File fileformated = new File(ontologyFileName);
		RDFXMLDocumentFormat rdfFormat = new RDFXMLDocumentFormat();
		try {
			manager.saveOntology(ontology, rdfFormat, IRI.create(fileformated.toURI()));
		} catch (OWLOntologyStorageException e) {

		}
	}

	public OWLClass getAgentClass() {
		return agentClass;
	}

	public OWLClass getEntityClass() {
		return entityClass;
	}

	public OWLClass getOrganizationClass() {
		return organizationClass;
	}

	public OWLClass getActivityClass() {
		return activityClass;
	}

	public OWLClass getCollectionClass() {
		return collectionClass;
	}

	public OWLClass getUsageClass() {
		return usageClass;
	}

	public OWLDataProperty getGivenNameDataProperty() {
		return givenNameDataProperty;
	}

	public OWLDataProperty getMboxDataProperty() {
		return mboxDataProperty;
	}

	public OWLDataProperty getStartedAtTimeDataProperty() {
		return startedAtTimeDataProperty;
	}

	public OWLDataProperty getGeneratedAtTimeDataProperty() {
		return generatedAtTimeDataProperty;
	}

	public OWLDataProperty getAtTimeDataProperty() {
		return atTimeDataProperty;
	}

	public OWLDataProperty getEndedAtTimeDataProperty() {
		return endedAtTimeDataProperty;
	}

	public OWLDataProperty getSha1DataProperty() {
		return sha1DataProperty;
	}

	public OWLDataProperty getLocationDataProperty() {
		return locationDataProperty;
	}

	public OWLObjectProperty getQualifiedUsageObjectProperty() {
		return qualifiedUsageObjectProperty;
	}

	public OWLObjectProperty getQualifiedGenerationObjectProperty() {
		return qualifiedGenerationObjectProperty;
	}

	public OWLObjectProperty getGeneratedObjectProperty() {
		return generatedObjectProperty;
	}

	public OWLObjectProperty getActedOnBehalfOfObjectProperty() {
		return actedOnBehalfOfObjectProperty;
	}

	public OWLObjectProperty getWasGeneratedByObjectProperty() {
		return wasGeneratedByObjectProperty;
	}

	public OWLObjectProperty getWasAssociatedWithObjectProperty() {
		return wasAssociatedWithObjectProperty;
	}

	public OWLObjectProperty getWasDerivedFromObjectProperty() {
		return wasDerivedFromObjectProperty;
	}

	public OWLObjectProperty getWasAttributedToObjectProperty() {
		return wasAttributedToObjectProperty;
	}

	public OWLObjectProperty getUsedObjectProperty() {
		return usedObjectProperty;
	}

	public OWLObjectProperty getHadMemberObjectProperty() {
		return hadMemberObjectProperty;
	}

	public OWLObjectProperty getWasStartedByObjectProperty() {
		return wasStartedByObjectProperty;
	}

	public OWLClass getOntologyClass(String className) {
		if (className.equals(entityClass.getIRI().getShortForm()))
			return entityClass;
		else if (className.equals(activityClass.getIRI().getShortForm()))
			return activityClass;
		else if (className.equals(organizationClass.getIRI().getShortForm()))
			return organizationClass;
		else if (className.equals(agentClass.getIRI().getShortForm()))
			return agentClass;
		else if (className.equals(collectionClass.getIRI().getShortForm()))
			return collectionClass;
		else
			return usageClass;
	}

	public void getOntologyDataPropertyValue(String individualName, OWLDataProperty... owlDataProperties) {
		OWLNamedIndividual individual = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + individualName));

		if (null == owlDataProperties) {
			EntitySearcher.getDataPropertyValues(individual, ontology).entries().forEach(i -> {
				System.out.println(i.getKey().asOWLDataProperty().getIRI() + "::" + i.getValue().getLiteral());
			});
		} else {
			for (OWLDataProperty getDataProp : owlDataProperties) {
				reasoner.getDataPropertyValues(individual, getDataProp).forEach(i -> {
					System.out.println(getDataProp.getIRI().getShortForm() + "::" + i.getLiteral());
				});
			}
		}
	}

	public void getOntologyObjectPropertyValue(String individualName, OWLObjectProperty... owlObjectProperties) {
		OWLNamedIndividual individual = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + individualName));

		if (null == owlObjectProperties) {
			EntitySearcher.getObjectPropertyValues(individual, ontology).entries().forEach(i -> {
				System.out.println(i.getKey().asOWLObjectProperty().getIRI().getShortForm() + "::"
						+ i.getValue().asOWLNamedIndividual().getIRI().getShortForm());
			});
		} else {
			for (OWLObjectProperty getObjectProp : owlObjectProperties) {
				reasoner.getObjectPropertyValues(individual, getObjectProp).entities().forEach(i -> {
					System.out.println(getObjectProp.getIRI().getShortForm() + "::" + i.getIRI().getShortForm());
				});
			}
		}
	}

	public void addOntologyClassAssertionAxiom(String individualName, OWLClass... classNames) {
		OWLNamedIndividual individual = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + individualName));

		for (OWLClass addClass : classNames) {
			manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(addClass, individual));
		}
	}

	public void addOntologyDataPropertyAssertionAxiom(String individualName, String value,
			OWLDataProperty dataProperty) {
		OWLNamedIndividual individual = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + individualName));
		OWLDataPropertyAssertionAxiom dataPropertyAssertion = factory.getOWLDataPropertyAssertionAxiom(dataProperty,
				individual, value);

		manager.addAxiom(ontology, dataPropertyAssertion);
	}

	public void addOntologyDataPropertyAssertionAxiomLiteral(String individualName, OWLDataProperty dataProperty,
			String literalValue, IRI datatypeIRI) {
		OWLLiteral owlLiteral = factory.getOWLLiteral(literalValue, factory.getOWLDatatype(datatypeIRI));
		OWLNamedIndividual individual = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + individualName));
		OWLDataPropertyAssertionAxiom dataPropertyAssertion = factory.getOWLDataPropertyAssertionAxiom(dataProperty,
				individual, owlLiteral);

		manager.addAxiom(ontology, dataPropertyAssertion);
	}

	public void addOntologyObjectPropertyAssertionAxiom(String individualNameActing, String individualNameReceiving,
			OWLObjectProperty... objectProperties) {
		OWLNamedIndividual subject = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + individualNameActing));
		OWLNamedIndividual object = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + individualNameReceiving));

		for (OWLObjectProperty addPredicate : objectProperties) {
			OWLObjectPropertyAssertionAxiom propertyAssertion = factory.getOWLObjectPropertyAssertionAxiom(addPredicate,
					subject, object);
			manager.addAxiom(ontology, propertyAssertion);
		}
	}

	public void addAtTimeObjectPropertyAnnotation(String individualNameActing, String individualNameReceiving,
			OWLObjectProperty objP, String literalValue, IRI datatypeIRI) {
		OWLNamedIndividual subject = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + individualNameActing));
		OWLNamedIndividual object = factory.getOWLNamedIndividual(IRI.create(ontologyIRI + individualNameReceiving));

		OWLLiteral owlLiteral = factory.getOWLLiteral(literalValue, factory.getOWLDatatype(datatypeIRI));
		OWLAnnotation anno = factory.getOWLAnnotation(usedAtTimeAnnotationProperty, owlLiteral);
		OWLObjectPropertyAssertionAxiom propertyAssertion = factory.getOWLObjectPropertyAssertionAxiom(objP, subject,
				object, Collections.singleton(anno));

		AddAxiom addAxiomChange = new AddAxiom(ontology, propertyAssertion);
		manager.applyChange(addAxiomChange);
	}

}
