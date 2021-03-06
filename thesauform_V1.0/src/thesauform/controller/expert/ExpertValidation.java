package thesauform.controller.expert;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;

import java.io.IOException;

import thesauform.beans.AnnotationConcept;
import thesauform.beans.Person;
import thesauform.beans.TraitConceptVote;
import thesauform.beans.TraitVoteValue;
import thesauform.model.Format;
import thesauform.model.SkosTraitModel;
import thesauform.model.ThesauformConfiguration;
import thesauform.model.vocabularies.ChangeVoc;
import thesauform.model.vocabularies.SkosVoc;
import thesauform.model.vocabularies.SkosXLVoc;
import thesauform.model.vocabularies.TraitVocTemp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/*
 * TODO definition selection should be done with reference
 * @Patch1: definition with reference would be on form def__ref for annotation : check if ref associated -> for selection, for the view, for printing
 */


/**
 * Servlet implementation class servletExpertValidation
 */
@WebServlet("/expert/validation")
public class ExpertValidation extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4569393576368232892L;

	public static final String VUE_SUCCESS = "/WEB-INF/scripts/expertValidation.jsp";
	private static final String COMMENT_NAME = "Comment";
	private static final String GET_PARAMETER = "trait";
	private static final String ERROR_PARAMETER = "parameter";
	private static final String ERROR_MESSAGE_PARAMETER = "parameter " + GET_PARAMETER + " empty";
	private static final String ERROR_CONCEPT = "concept";
	private static final String ERROR_MESSAGE_CONCEPT = "Cannot find trait in model";
	private static final String ERROR_URI = "uri";
	private static final String EMPTY_COMMENT = "No comment";
	private static final String ERROR_SYNONYMS = "synonyms";
	private static final String EMPTY_SYNONYM = "No synonym";
	private static final String ERROR_RELATEDS = "relateds";
	private static final String EMPTY_RELATED = "No related concept";	
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// manage errors messages
		Map<String, String> errors = new HashMap<>();
		// trait model
		SkosTraitModel traitModel = null;
		// test if a session is initialized
		HttpSession session = request.getSession(false);
		if (session != null) {
			if (session.getAttribute(ThesauformConfiguration.USR_SESSION) instanceof Person) {
				Person user = (Person) session.getAttribute(ThesauformConfiguration.USR_SESSION);
				boolean authentificationStatus = user.getAuthenticated();
				if (authentificationStatus) {
					if(ThesauformConfiguration.database) {
						traitModel = new SkosTraitModel(ThesauformConfiguration.data_file);
					}
					else {
						traitModel = new SkosTraitModel(getServletContext().getRealPath(ThesauformConfiguration.data_file));
					}
					//// do treatment
					// bean for the view
					TraitConceptVote myTraitVote = new TraitConceptVote();
					// insert vote note
					Integer insertVote = null;
					// existence of delete
					List<String> deleteList = null;
					// delete vote note
					Integer deleteVote = null;
					//get vote for each property
					Map<String, Map<String, TraitVoteValue>> nameVoteMap = new HashMap<String, Map<String, TraitVoteValue>>();
					Map<String, Map<String, TraitVoteValue>> definitionVoteMap = new HashMap<String, Map<String, TraitVoteValue>>();
					Map<String, Map<String, TraitVoteValue>> referenceVoteMap = new HashMap<String, Map<String, TraitVoteValue>>();
					Map<String, Map<String, TraitVoteValue>> abbreviationVoteMap = new HashMap<String, Map<String, TraitVoteValue>>();
					Map<String, Map<String, TraitVoteValue>> categoryVoteMap = new HashMap<String, Map<String, TraitVoteValue>>();
					Map<String, Map<String, TraitVoteValue>> unitVoteMap = new HashMap<String, Map<String, TraitVoteValue>>();
					Map<String, Map<String, TraitVoteValue>> synonymVoteMap = new HashMap<String, Map<String, TraitVoteValue>>();
					Map<String, Map<String, TraitVoteValue>> relatedVoteMap = new HashMap<String, Map<String, TraitVoteValue>>();
					List<AnnotationConcept> commentList = new ArrayList<AnnotationConcept>();
					//count vote number for the user
					Integer countNbVote = 0;
					//count number of concept the user have voted for
					Integer countNbConceptVoted = 0;
					// get parameter
					String traitName = request.getParameter(GET_PARAMETER);
					//TODO detect encoding
					traitName = java.net.URLDecoder.decode(traitName, "UTF-8");
					try {
						if (traitName == null) {
							throw new Exception(ERROR_MESSAGE_PARAMETER);
						}
					} catch (Exception e) {
						errors.put(ERROR_PARAMETER, e.getMessage());
					}
					Resource concept = traitModel.getResource(Format.formatName(traitName));
					if (concept != null) {
						// set trait name
						try {
							myTraitVote.setUri(traitName);
						} catch (Exception e) {
							errors.put(ERROR_URI, e.getMessage() + " for " + traitName);
						}
						//set vote number for the user
						countNbVote = traitModel.countVotePerson(concept,user.getName());
						//set concept voted number for the user
						countNbConceptVoted = traitModel.countConceptVotedPerson(user.getName());

						//// get insert annotation if exists
						// get all insert members
						Map<String, List<String>> insertMap = traitModel.getAnnotation(Format.formatName(traitName),
								"insert");
						Iterator<Entry<String, List<String>>> insertIt = insertMap.entrySet().iterator();
						try {
							// test if the concept is inserted
							if (insertIt.hasNext()) {
								myTraitVote.setIsInserted(true);
								// get vote note by user
								insertVote = traitModel.countVote(concept, ChangeVoc.insert, user.getName(), "insert");
								myTraitVote.setNbInsertVote(insertVote);
							} else {
								myTraitVote.setIsInserted(false);
							}
						} catch (Exception e) {

						}
						//// get delete annotation with people if any
						// get all delete members
						deleteList = traitModel.getAllDelete(traitName);
						Iterator<String> deleteIt = deleteList.iterator();
						try {
							// test if the concept is deleted
							if (deleteIt.hasNext()) {
								// set the list of contributors
								myTraitVote.setDeleteList(deleteList);
								// get vote note by user
								deleteVote = traitModel.countVote(concept, ChangeVoc.delete, user.getName(), "delete");
								myTraitVote.setNbDeleteVote(deleteVote);
							} else {
							}
						} catch (Exception e) {

						}
						//// get update annotation
						// get all update properties vote
						Map<String, List<String>> updateMap = traitModel.getAnnotation(Format.formatName(traitName), "update");
						Iterator<Entry<String, List<String>>> updateIt = updateMap.entrySet().iterator();
						// set current name
						String name = traitModel.getLabelLiteralForm(traitModel.getPrefLabel(concept));
						try {
							// test if not empty
							if (name != null && !name.isEmpty()) {
								//get vote value
								Integer propertyVoteValue = traitModel.countVote(concept, SkosXLVoc.prefLabel, user.getName(), name);
								//get comment value
								String commentVote = traitModel.getVoteComment(name, SkosXLVoc.prefLabel.getLocalName(), user.getName(), name);
								//create bean
								TraitVoteValue propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
								Map<String, TraitVoteValue> voteMapTmp = new HashMap<String, TraitVoteValue>();
								voteMapTmp.put(name, propertyVote);
								nameVoteMap.put("current", voteMapTmp);
							}
						} catch (Exception e) {
							System.out.println(e.getMessage());
						}
						// get current definition
						try {
							String definition = traitModel.getValue(traitModel.getDefinition(concept));
							// get current reference linked to definition
							String reference  = traitModel.getValue(traitModel.getReference(traitModel.getDefinition(concept)));
							// test if not empty
							if (definition != null && !definition.isEmpty()) {
								Map<String, TraitVoteValue> voteMapTmp = new HashMap<String, TraitVoteValue>();
								if(reference != null && !reference.isEmpty()) {
									//get vote value
									Integer propertyVoteValue = traitModel.countVote(concept, SkosVoc.definition, user.getName(), definition + "__" + reference);
									//get comment value
									String commentVote = traitModel.getVoteComment(name, SkosVoc.definition.getLocalName(), user.getName(), definition);
									//create bean
									TraitVoteValue propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
									voteMapTmp.put(definition + " (ref: " + reference + ")", propertyVote);
								}
								else {
									//get vote value
									Integer propertyVoteValue = traitModel.countVote(concept, SkosVoc.definition, user.getName(), definition);
									//get comment value
									String commentVote = traitModel.getVoteComment(name, SkosVoc.definition.getLocalName(), user.getName(), definition);
									//create bean
									TraitVoteValue propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
									voteMapTmp.put(definition, propertyVote);
								}
								definitionVoteMap.put("current", voteMapTmp);
							}
						} catch (Exception e) {
						}
						/*						
 						 * // get current reference
						 * try {
						 * 	String reference = traitModel
						 * 			.getValue(traitModel.getReference(traitModel.getDefinition(concept)));
						 * 	// test if not empty
						 * 	if (reference != null && !reference.isEmpty()) {
						 * 		Integer propertyVote = traitModel.countVote(concept, TraitVocTemp.reference, user.getName(), reference);
						 * 		Map<String, TraitVoteValue> voteMapTmp = new HashMap<String, TraitVoteValue>();
						 * 		voteMapTmp.put(reference, propertyVote);
						 * 		referenceVoteMap.put("current", voteMapTmp);
						 * 	}
						 * } catch (Exception e) {
						 * }
						 */
						// get abbreviation
						try {
							String abbreviation = traitModel
									.getLabelLiteralForm(traitModel.getAbbreviation(traitModel.getPrefLabel(concept)));
							// test if not empty
							if (abbreviation != null && !abbreviation.isEmpty()) {
								//get vote value
								Integer propertyVoteValue = traitModel.countVote(concept, TraitVocTemp.abbreviation, user.getName(), abbreviation);
								//get comment value
								String commentVote = traitModel.getVoteComment(name, TraitVocTemp.abbreviation.getLocalName(), user.getName(), abbreviation);
								//create bean
								TraitVoteValue propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
								Map<String, TraitVoteValue> voteMapTmp = new HashMap<String, TraitVoteValue>();
								voteMapTmp.put(abbreviation, propertyVote);
								abbreviationVoteMap.put("current", voteMapTmp);
							}
						} catch (Exception e) {
						}
						// get unit
						try {
							String unit = traitModel.getValue(traitModel.getUnit(concept));
							// test if not empty
							if (unit != null && !unit.isEmpty()) {
								//get vote value
								Integer propertyVoteValue = traitModel.countVote(concept, TraitVocTemp.prefUnit, user.getName(), unit);
								//get comment value
								String commentVote = traitModel.getVoteComment(name, TraitVocTemp.prefUnit.getLocalName(), user.getName(), unit);
								//create bean
								TraitVoteValue propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
								Map<String, TraitVoteValue> voteMapTmp = new HashMap<String, TraitVoteValue>();
								voteMapTmp.put(unit, propertyVote);
								unitVoteMap.put("current", voteMapTmp);
							}
						} catch (Exception e) {
						}
						// get parent/category
						try {
							StmtIterator parentIt = traitModel.getAllParent(concept);
							if (parentIt.hasNext()) {
								Map<String, TraitVoteValue> voteMapTmp = new HashMap<String, TraitVoteValue>();
								while (parentIt.hasNext()) {
									Statement st = parentIt.next();
									Resource parent = st.getObject().as(Resource.class);
									String category = parent.getLocalName();
									// test if not empty
									if (category != null && !category.isEmpty()) {
										//get vote value
										Integer propertyVoteValue = traitModel.countVote(concept, SkosVoc.broaderTransitive, user.getName(), category);
										//get comment value
										String commentVote = traitModel.getVoteComment(name, SkosVoc.broaderTransitive.getLocalName(), user.getName(), category);
										//create bean
										TraitVoteValue propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
										voteMapTmp.put(category, propertyVote);
									}
								}
								categoryVoteMap.put("current", voteMapTmp);
							}
						} catch (Exception e) {
						}
						// get comments
						try {
							// get all comments in a list
							StmtIterator commentRawList = traitModel.getComment(concept);
							// test if there is at least one comment
							if (commentRawList.hasNext()) {
								Integer cpt = 0;
								// treat each comment
								while (commentRawList.hasNext()) {
									cpt++;
									// create AnnotationConcept object
									AnnotationConcept myAnnotationTmp = new AnnotationConcept();
									// get comment model object
									Resource commentRawObject = commentRawList.next().getObject().as(Resource.class);
									myAnnotationTmp.setProperty(COMMENT_NAME + cpt);
									myAnnotationTmp.setCreator(commentRawObject.listProperties(DC.creator).next()
											.getObject().as(Resource.class).listProperties(FOAF.name).next().getObject()
											.asNode().getLiteralLexicalForm());
									myAnnotationTmp.setValue(Format.printDef(commentRawObject.listProperties(RDF.value)
											.next().getObject().asNode().getLiteralLexicalForm()));
									commentList.add(myAnnotationTmp);
								}
								// set bean property
								myTraitVote.setCommentList(commentList);
							} else {
								throw new Exception(EMPTY_COMMENT);
							}
						} catch (Exception e) {
						}
						// get all synonyms
						try {
							//validated synonym
							StmtIterator synonymValidatedIt = traitModel.getAllValidatedAltLabel(concept);
							if (synonymValidatedIt.hasNext()) {
								Map<String, TraitVoteValue> voteMapTmp = new HashMap<String, TraitVoteValue>();
								while (synonymValidatedIt.hasNext()) {
									Statement st = synonymValidatedIt.next();
									String synonym = st.getObject().asNode().getLiteralLexicalForm();
									//get vote value
									Integer propertyVoteValue = traitModel.countVote(concept, SkosVoc.altLabel, user.getName(), synonym);
									//get comment value
									String commentVote = traitModel.getVoteComment(name, "validatedAltLabel", user.getName(), synonym);
									//create bean
									TraitVoteValue propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
									voteMapTmp.put(synonym, propertyVote);
								}
								synonymVoteMap.put("current", voteMapTmp);
							}
							if(!synonymVoteMap.isEmpty()) {
								// set bean property
								myTraitVote.setSynonymList(synonymVoteMap);
							} else {
								throw new Exception(EMPTY_SYNONYM);
							}
						} catch (Exception e) {
							errors.put(ERROR_SYNONYMS, e.getMessage() + " for " + traitName);
						}
						// get all related
						try {
							StmtIterator RelatedIt = traitModel.getAllValidatedRelated(concept);
							if (RelatedIt.hasNext()) {
								Map<String, TraitVoteValue> voteMapTmp = new HashMap<String, TraitVoteValue>();
								while (RelatedIt.hasNext()) {
									Statement st = RelatedIt.next();
									Resource Related = st.getObject().as(Resource.class);
									String related = traitModel.getLabelLiteralForm(traitModel.getPrefLabel(Related));
									//get vote value
									Integer propertyVoteValue = traitModel.countVote(concept, SkosVoc.related, user.getName(), related);
									//get comment value
									String commentVote = traitModel.getVoteComment(name, SkosVoc.related.getLocalName(), user.getName(), related);
									//create bean
									TraitVoteValue propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
									voteMapTmp.put(related, propertyVote);
								}
								relatedVoteMap.put("current", voteMapTmp);
								// set bean property
								myTraitVote.setRelatedList(relatedVoteMap);
								
							} else {
								throw new Exception(EMPTY_RELATED);
							}
						} catch (Exception e) {
							errors.put(ERROR_RELATEDS, e.getMessage() + " for " + traitName);
						}
						// test if the concept is updated
						if (updateIt.hasNext()) {
							// for each property
							while (updateIt.hasNext()) {
								// update property value/list
								Map<String, Map<String, TraitVoteValue>> propertyVoteMap = new HashMap<>();
								// get the properties lists
								Entry<String, List<String>> updatePair = updateIt.next();
								String property = updatePair.getKey();
								List<String> valueList = (List<String>) updatePair.getValue();
								Iterator<String> valueIt = valueList.iterator();
								try {
									Map<String, TraitVoteValue> voteMapTmp = new HashMap<String, TraitVoteValue>();
									while (valueIt.hasNext()) {
										// get property value
										String value = valueIt.next();
										// get vote note by user for each value
										Integer propertyVoteValue = 0;
										String commentVote = ""; 
										TraitVoteValue propertyVote = null;
										switch (property) {
										case "name":
											//get vote value
											propertyVoteValue = traitModel.countVote(concept, SkosXLVoc.prefLabel, user.getName(), value);
											//get comment value
											commentVote = traitModel.getVoteComment(name, SkosXLVoc.prefLabel.getLocalName(), user.getName(), value);
											//create bean
											propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
											break;
										case "unit":
											//get vote value
											propertyVoteValue = traitModel.countVote(concept, TraitVocTemp.prefUnit, user.getName(), value);
											//get comment value
											commentVote = traitModel.getVoteComment(name, TraitVocTemp.prefUnit.getLocalName(), user.getName(), value);
											//create bean
											propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
											break;
										case "reference":
											//get vote value
											propertyVoteValue = traitModel.countVote(concept, TraitVocTemp.reference, user.getName(), value);
											//get comment value
											commentVote = traitModel.getVoteComment(name, TraitVocTemp.reference.getLocalName(), user.getName(), value);
											//create bean
											propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
											break;
										case "definition":
											//get vote value
											propertyVoteValue = traitModel.countVote(concept, SkosVoc.definition, user.getName(), value);
											//get comment value
											commentVote = traitModel.getVoteComment(name, SkosVoc.definition.getLocalName(), user.getName(), value);
											//create bean
											propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
											break;
										case "abbreviation":
											//get vote value
											propertyVoteValue = traitModel.countVote(concept, TraitVocTemp.abbreviation, user.getName(), 
													value);
											//get comment value
											commentVote = traitModel.getVoteComment(name, TraitVocTemp.abbreviation.getLocalName(), user.getName(), value);
											//create bean
											propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
											break;
										case "category":
											//get vote value
											propertyVoteValue = traitModel.countVote(concept, SkosVoc.broaderTransitive, user.getName(), 
													value);
											//get comment value
											commentVote = traitModel.getVoteComment(name, SkosVoc.broaderTransitive.getLocalName(), user.getName(), value);
											//create bean
											propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
											break;
										case "synonym":
											//get vote value
											propertyVoteValue = traitModel.countVote(concept, SkosXLVoc.altLabel, user.getName(), 
													value);
											//get comment value
											commentVote = traitModel.getVoteComment(name, SkosXLVoc.altLabel.getLocalName(), user.getName(), value);
											//create bean
											propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
											break;
										case "related":
											//get vote value
											propertyVoteValue = traitModel.countVote(concept, SkosVoc.related, user.getName(), 
													value);
											//get comment value
											commentVote = traitModel.getVoteComment(name, SkosVoc.related.getLocalName(), user.getName(), value);
											//create bean
											propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
											break;
										default:
											propertyVoteMap.put("proposed", voteMapTmp);
											//get vote value
											propertyVoteValue = traitModel.countVote(concept, ChangeVoc.update, user.getName(), value);
											//get comment value
											commentVote = traitModel.getVoteComment(name, ChangeVoc.update.getLocalName(), user.getName(), value);
											//create bean
											propertyVote = new TraitVoteValue(propertyVoteValue,commentVote);
											break;
										}
										voteMapTmp.put(value, propertyVote);
									}
									switch (property) {
									case "name":
										nameVoteMap.put("proposed", voteMapTmp);
										propertyVoteMap = nameVoteMap;
										break;
									case "unit":
										unitVoteMap.put("proposed", voteMapTmp);
										propertyVoteMap = unitVoteMap;
										break;
									case "reference":
										referenceVoteMap.put("proposed", voteMapTmp);
										propertyVoteMap = referenceVoteMap;
										break;
									case "definition":
										definitionVoteMap.put("proposed", voteMapTmp);
										propertyVoteMap = definitionVoteMap;
										break;
									case "abbreviation":
										abbreviationVoteMap.put("proposed", voteMapTmp);
										propertyVoteMap = abbreviationVoteMap;
										break;
									case "category":
										categoryVoteMap.put("proposed", voteMapTmp);
										propertyVoteMap = categoryVoteMap;
										break;
									case "synonym":
										synonymVoteMap.put("proposed", voteMapTmp);
										propertyVoteMap = synonymVoteMap;
										break;
									case "related":
										relatedVoteMap.put("proposed", voteMapTmp);
										propertyVoteMap = relatedVoteMap;
										break;
									default:
										propertyVoteMap.put("proposed", voteMapTmp);
										myTraitVote.setPropertyList(property, propertyVoteMap);
										break;
									}
								} catch (Exception e) {
									// @TODO manage exception
									System.out.println(e.getMessage());
								}
							}
						} else {

						}

						// set bean vote
						try {
							String property = "name";
							Map<String, Map<String, TraitVoteValue>> propertyVoteMap = nameVoteMap;
							myTraitVote.setPropertyList(property, propertyVoteMap);
						} catch (Exception e) {
							System.out.println(e.getMessage());

						}
						try {
							String property = "unit";
							Map<String, Map<String, TraitVoteValue>> propertyVoteMap = unitVoteMap;
							myTraitVote.setPropertyList(property, propertyVoteMap);
						} catch (Exception e) {
							//System.out.println(e.getMessage());
						}
						try {
							String property = "reference";
							Map<String, Map<String, TraitVoteValue>> propertyVoteMap = referenceVoteMap;
							myTraitVote.setPropertyList(property, propertyVoteMap);
						} catch (Exception e) {
							//System.out.println(e.getMessage());
						}
						try {
							String property = "definition";
							Map<String, Map<String, TraitVoteValue>> propertyVoteMap = definitionVoteMap;
							myTraitVote.setPropertyList(property, propertyVoteMap);
						} catch (Exception e) {
							//System.out.println(e.getMessage());
						}
						try {
							String property = "abbreviation";
							Map<String, Map<String, TraitVoteValue>> propertyVoteMap = abbreviationVoteMap;
							myTraitVote.setPropertyList(property, propertyVoteMap);
						} catch (Exception e) {
							//System.out.println(e.getMessage());
						}
						try {
							String property = "category";
							Map<String, Map<String, TraitVoteValue>> propertyVoteMap = categoryVoteMap;
							myTraitVote.setPropertyList(property, propertyVoteMap);
						} catch (Exception e) {
							//System.out.println(e.getMessage());
						}
						try {
							String property = "synonym";
							Map<String, Map<String, TraitVoteValue>> propertyVoteMap = synonymVoteMap;
							myTraitVote.setPropertyList(property, propertyVoteMap);
						} catch (Exception e) {
							//System.out.println(e.getMessage());
						}
						try {
							String property = "related";
							Map<String, Map<String, TraitVoteValue>> propertyVoteMap = relatedVoteMap;
							myTraitVote.setPropertyList(property, propertyVoteMap);
						} catch (Exception e) {
							//System.out.println(e.getMessage());
						}
					} else {
						errors.put(ERROR_CONCEPT, ERROR_MESSAGE_CONCEPT + ": " + traitName);
					}
					// set parameter for view
					request.setAttribute("myTraitVote", myTraitVote);
					request.setAttribute("user", user.getName());
					request.setAttribute("count", countNbVote);
					request.setAttribute("countVoted", countNbConceptVoted);
					this.getServletContext().getRequestDispatcher(VUE_SUCCESS).forward(request, response);
				} else {
					// re-authenticate
					this.getServletContext().getRequestDispatcher(ThesauformConfiguration.VUE_FAILED).forward(request,
							response);
				}
			} else {
				// re-authenticate
				this.getServletContext().getRequestDispatcher(ThesauformConfiguration.VUE_FAILED).forward(request,
						response);
			}
		} else {
			// re-authenticate
			this.getServletContext().getRequestDispatcher(ThesauformConfiguration.VUE_FAILED).forward(request,
					response);
		}
	}

}
