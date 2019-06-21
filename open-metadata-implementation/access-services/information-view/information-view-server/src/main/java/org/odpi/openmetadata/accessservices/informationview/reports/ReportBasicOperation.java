/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.accessservices.informationview.reports;

import org.odpi.openmetadata.accessservices.informationview.contentmanager.OMEntityDao;
import org.odpi.openmetadata.accessservices.informationview.events.BusinessTerm;
import org.odpi.openmetadata.accessservices.informationview.events.ReportColumn;
import org.odpi.openmetadata.accessservices.informationview.events.ReportElement;
import org.odpi.openmetadata.accessservices.informationview.events.ReportSection;
import org.odpi.openmetadata.accessservices.informationview.events.Source;
import org.odpi.openmetadata.accessservices.informationview.ffdc.InformationViewErrorCode;
import org.odpi.openmetadata.accessservices.informationview.ffdc.exceptions.runtime.AddRelationshipException;
import org.odpi.openmetadata.accessservices.informationview.ffdc.exceptions.runtime.ReportElementCreationException;
import org.odpi.openmetadata.accessservices.informationview.lookup.LookupHelper;
import org.odpi.openmetadata.accessservices.informationview.utils.Constants;
import org.odpi.openmetadata.accessservices.informationview.utils.EntityPropertiesBuilder;
import org.odpi.openmetadata.accessservices.informationview.utils.QualifiedNameUtils;
import org.odpi.openmetadata.repositoryservices.auditlog.OMRSAuditLog;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProperties;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.ClassificationErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.EntityNotKnownException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.FunctionNotSupportedException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.InvalidParameterException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.PagingErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.PropertyErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.RepositoryErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.StatusNotSupportedException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeDefNotKnownException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.UserNotAuthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;

import static org.odpi.openmetadata.accessservices.informationview.ffdc.ExceptionHandler.throwSourceNotFoundException;

public abstract class ReportBasicOperation extends BasicOperation{

    private static final Logger log = LoggerFactory.getLogger(ReportBasicOperation.class);

    protected final EntityReferenceResolver entityReferenceResolver;

    public ReportBasicOperation(OMEntityDao omEntityDao, LookupHelper lookupHelper, OMRSRepositoryHelper helper, OMRSAuditLog auditLog) {
        super(omEntityDao, helper, auditLog);
        this.entityReferenceResolver = new EntityReferenceResolver(lookupHelper, omEntityDao);
    }


    /**
     *
     * @param qualifiedNameForParent qualified name of the parent element
     * @param parentGuid guid of the parent element
     * @param allElements all elements linked to the current element
     */
    public void addElements(String qualifiedNameForParent, String parentGuid, List<ReportElement> allElements) {
        if (allElements == null || allElements.isEmpty())
            return;
        allElements.parallelStream().forEach(e -> addReportElement(qualifiedNameForParent, parentGuid, e));
    }


    /**
     *
     * @param qualifiedNameForParent qualified name of the parent element
     * @param parentGuid guid of the parent element
     * @param element object describing the current element
     */
    public void addReportElement(String qualifiedNameForParent, String parentGuid, ReportElement element) {
        try {
            if (element instanceof ReportSection) {
                addReportSection(qualifiedNameForParent, parentGuid, (ReportSection) element);
            } else if (element instanceof ReportColumn) {
                addReportColumn(qualifiedNameForParent, parentGuid, (ReportColumn) element);
            }
        } catch (PagingErrorException | TypeDefNotKnownException | PropertyErrorException | EntityNotKnownException | UserNotAuthorizedException | StatusNotSupportedException | InvalidParameterException | FunctionNotSupportedException | RepositoryErrorException | TypeErrorException | ClassificationErrorException e) {
            log.error("Exception creating report element", e);
            throw new ReportElementCreationException(ReportBasicOperation.class.getName(),
                                                    InformationViewErrorCode.REPORT_ELEMENT_CREATION_EXCEPTION.getFormattedErrorMessage(element.toString(), e.getMessage()),
                                                    InformationViewErrorCode.REPORT_ELEMENT_CREATION_EXCEPTION.getSystemAction(),
                                                    InformationViewErrorCode.REPORT_ELEMENT_CREATION_EXCEPTION.getUserAction(),
                                                    e);

        }
    }

    /**
     *
     * @param qualifiedNameForParent qualified name of the parent element
     * @param parentGuid guid of the parent element
     * @param reportSection object describing the current section in the report
     * @throws InvalidParameterException
     * @throws PropertyErrorException
     * @throws TypeDefNotKnownException
     * @throws RepositoryErrorException
     * @throws EntityNotKnownException
     * @throws FunctionNotSupportedException
     * @throws PagingErrorException
     * @throws ClassificationErrorException
     * @throws UserNotAuthorizedException
     * @throws TypeErrorException
     * @throws StatusNotSupportedException
     */
    private void addReportSection(String qualifiedNameForParent, String parentGuid, ReportSection reportSection) throws InvalidParameterException, PropertyErrorException, TypeDefNotKnownException, RepositoryErrorException, EntityNotKnownException, FunctionNotSupportedException, PagingErrorException, ClassificationErrorException, UserNotAuthorizedException, TypeErrorException, StatusNotSupportedException {
        EntityDetail typeEntity = addSectionAndSectionType(qualifiedNameForParent, parentGuid, reportSection);
        String qualifiedNameForSection = QualifiedNameUtils.buildQualifiedName(qualifiedNameForParent, Constants.DOCUMENT_SCHEMA_ATTRIBUTE, reportSection.getName());
        addElements(qualifiedNameForSection, typeEntity.getGUID(), reportSection.getElements());
    }


    /**
     *
     * @param qualifiedNameForParent qualified name of the parent element
     * @param parentGuid guid of the parent element
     * @param reportSection object describing the current section in the report
     * @return
     * @throws InvalidParameterException
     * @throws StatusNotSupportedException
     * @throws PropertyErrorException
     * @throws EntityNotKnownException
     * @throws FunctionNotSupportedException
     * @throws PagingErrorException
     * @throws ClassificationErrorException
     * @throws UserNotAuthorizedException
     * @throws TypeErrorException
     * @throws RepositoryErrorException
     * @throws TypeDefNotKnownException
     */
    protected EntityDetail addSectionAndSectionType(String qualifiedNameForParent, String parentGuid, ReportSection reportSection) throws InvalidParameterException, StatusNotSupportedException, PropertyErrorException, EntityNotKnownException, FunctionNotSupportedException, PagingErrorException, ClassificationErrorException, UserNotAuthorizedException, TypeErrorException, RepositoryErrorException, TypeDefNotKnownException {

        String qualifiedNameForSection = QualifiedNameUtils.buildQualifiedName(qualifiedNameForParent, Constants.DOCUMENT_SCHEMA_ATTRIBUTE, reportSection.getName());
        InstanceProperties sectionProperties = new EntityPropertiesBuilder()
                .withStringProperty(Constants.QUALIFIED_NAME, qualifiedNameForSection)
                .withStringProperty(Constants.ATTRIBUTE_NAME, reportSection.getName())
                .build();
        EntityDetail sectionEntity = omEntityDao.addEntity(Constants.DOCUMENT_SCHEMA_ATTRIBUTE,
                                                                    qualifiedNameForSection,
                                                                    sectionProperties,
                                                        false);

        omEntityDao.addRelationship(Constants.ATTRIBUTE_FOR_SCHEMA,
                                    parentGuid,
                                    sectionEntity.getGUID(),
                                    new InstanceProperties());

        String qualifiedNameForSectionType = buildQualifiedNameForSchemaType(qualifiedNameForParent, Constants.DOCUMENT_SCHEMA_TYPE, reportSection);
        InstanceProperties schemaAttributeTypeProperties = new InstanceProperties();
        schemaAttributeTypeProperties = helper.addStringPropertyToInstance(Constants.INFORMATION_VIEW_OMAS_NAME, schemaAttributeTypeProperties, Constants.QUALIFIED_NAME, qualifiedNameForSectionType, "addSchemaType");
        return createSchemaType(Constants.DOCUMENT_SCHEMA_TYPE,  qualifiedNameForSectionType, schemaAttributeTypeProperties, Constants.SCHEMA_ATTRIBUTE_TYPE, sectionEntity.getGUID() );
    }


    /**
     *
     * @param qualifiedNameForParent qualified name of the parent element
     * @param parentGuid guid of the parent element
     * @param reportColumn object describing the current column in the report
     * @return
     * @throws InvalidParameterException
     * @throws TypeErrorException
     * @throws TypeDefNotKnownException
     * @throws PropertyErrorException
     * @throws EntityNotKnownException
     * @throws FunctionNotSupportedException
     * @throws PagingErrorException
     * @throws ClassificationErrorException
     * @throws UserNotAuthorizedException
     * @throws RepositoryErrorException
     * @throws StatusNotSupportedException
     */
    protected EntityDetail addReportColumn(String qualifiedNameForParent, String parentGuid, ReportColumn reportColumn) throws InvalidParameterException, TypeErrorException, PropertyErrorException, EntityNotKnownException, FunctionNotSupportedException, PagingErrorException, ClassificationErrorException, UserNotAuthorizedException, RepositoryErrorException, StatusNotSupportedException {

        String qualifiedNameForColumn = QualifiedNameUtils.buildQualifiedName(qualifiedNameForParent, Constants.DERIVED_SCHEMA_ATTRIBUTE, reportColumn.getName());
        InstanceProperties columnProperties = new EntityPropertiesBuilder()
                                                                        .withStringProperty(Constants.QUALIFIED_NAME, qualifiedNameForColumn)
                                                                        .withStringProperty(Constants.ATTRIBUTE_NAME, reportColumn.getName())
                                                                        .withStringProperty(Constants.FORMULA, reportColumn.getFormula())
                                                                        .build();
        EntityDetail derivedColumnEntity = omEntityDao.addEntity(Constants.DERIVED_SCHEMA_ATTRIBUTE,
                                                                qualifiedNameForColumn,
                                                                columnProperties,
                                                    false);

        omEntityDao.addRelationship(Constants.ATTRIBUTE_FOR_SCHEMA,
                                    parentGuid,
                                    derivedColumnEntity.getGUID(),
                                    new InstanceProperties());


        addQueryTargets(reportColumn.getSources(), derivedColumnEntity);
        addSemanticAssignments(reportColumn.getBusinessTerms(), derivedColumnEntity);
        String qualifiedNameForColumnType = buildQualifiedNameForSchemaType(qualifiedNameForParent, Constants.SCHEMA_TYPE, reportColumn);
        InstanceProperties schemaAttributeTypeProperties = new InstanceProperties();
        schemaAttributeTypeProperties = helper.addStringPropertyToInstance(Constants.INFORMATION_VIEW_OMAS_NAME, schemaAttributeTypeProperties, Constants.QUALIFIED_NAME, qualifiedNameForColumnType, "addReportColumn");
        createSchemaType(Constants.SCHEMA_TYPE,  qualifiedNameForColumnType, schemaAttributeTypeProperties, Constants.SCHEMA_ATTRIBUTE_TYPE, derivedColumnEntity.getGUID() );

        return derivedColumnEntity;
    }

    private String buildQualifiedNameForSchemaType(String qualifiedNameForParent, String schemaType, ReportElement element) {
        return QualifiedNameUtils.buildQualifiedName(qualifiedNameForParent, schemaType, element.getName() + Constants.TYPE_SUFFIX);
    }




    /**
     *
     * @param sources list of sources describing the report column
     * @param derivedColumnEntity entity describing the derived column
     * @throws InvalidParameterException
     * @throws StatusNotSupportedException
     * @throws TypeErrorException
     * @throws FunctionNotSupportedException
     * @throws PropertyErrorException
     * @throws EntityNotKnownException
     * @throws TypeDefNotKnownException
     * @throws PagingErrorException
     * @throws UserNotAuthorizedException
     * @throws RepositoryErrorException
     */
    private void addQueryTargets(List<Source> sources, EntityDetail derivedColumnEntity) {
        for (Source source : sources) {
            String sourceColumnGUID = entityReferenceResolver.getSourceGuid(source);
            if (!StringUtils.isEmpty(sourceColumnGUID)) {
                log.debug("source {} for entity {} found.", source, derivedColumnEntity.getGUID());
                addQueryTarget(derivedColumnEntity.getGUID(), sourceColumnGUID, "");
            } else {
                throwSourceNotFoundException(source, null, ReportBasicOperation.class.getName());
            }
        }
    }


    /**
     * Create relationships of type SEMANTIC_ASSIGNMENT between the business terms and the entity representing the column
     * @param  businessTerms list of business terms
     * @param derivedColumnEntity entity describing the derived column
     */
    private void addSemanticAssignments(List<BusinessTerm> businessTerms, EntityDetail derivedColumnEntity){
        if(businessTerms != null && !businessTerms.isEmpty()) {
            businessTerms.stream().forEach(bt -> {
                addSemanticAssignment(bt, derivedColumnEntity);
            });
        }
    }

    private void addSemanticAssignment(BusinessTerm bt, EntityDetail derivedColumnEntity)  {
        String businessTermGuid;
        try {
            businessTermGuid = entityReferenceResolver.getBusinessTermGuid(bt);
            if (!StringUtils.isEmpty(businessTermGuid)) {
                omEntityDao.addRelationship(Constants.SEMANTIC_ASSIGNMENT,
                        derivedColumnEntity.getGUID(),
                        businessTermGuid,
                        new InstanceProperties());
            }
        } catch (UserNotAuthorizedException | FunctionNotSupportedException | InvalidParameterException | RepositoryErrorException | PropertyErrorException | TypeErrorException | PagingErrorException | StatusNotSupportedException | EntityNotKnownException e) {
            InformationViewErrorCode errorCode = InformationViewErrorCode.ADD_RELATIONSHIP_EXCEPTION;
            throw new AddRelationshipException(errorCode.getHttpErrorCode(), ReportUpdater.class.getName(),
                    errorCode.getFormattedErrorMessage(Constants.SEMANTIC_ASSIGNMENT, e.getMessage()),
                    errorCode.getSystemAction(),
                    errorCode.getUserAction(),
                    e);
        }
    }


}
