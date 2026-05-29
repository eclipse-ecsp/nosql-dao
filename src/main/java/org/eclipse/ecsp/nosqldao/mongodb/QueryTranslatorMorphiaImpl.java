/*
 *
 *
 *   *******************************************************************************
 *
 *     Copyright (c) 2023-24 Harman International
 *
 *
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *
 *     you may not use this file except in compliance with the License.
 *
 *     You may obtain a copy of the License at
 *
 *
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *
 *     Unless required by applicable law or agreed to in writing, software
 *
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *     See the License for the specific language governing permissions and
 *
 *     limitations under the License.
 *
 *
 *
 *     SPDX-License-Identifier: Apache-2.0
 *
 *    *******************************************************************************
 *
 *
 */

package org.eclipse.ecsp.nosqldao.mongodb;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.filters.Filters;
import org.eclipse.ecsp.entities.IgniteEntity;
import org.eclipse.ecsp.nosqldao.Coordinate;
import org.eclipse.ecsp.nosqldao.IgniteCriteria;
import org.eclipse.ecsp.nosqldao.IgniteCriteriaGroup;
import org.eclipse.ecsp.nosqldao.IgniteOrderBy;
import org.eclipse.ecsp.nosqldao.IgniteQuery;
import org.eclipse.ecsp.nosqldao.LogicalOperator;
import org.eclipse.ecsp.nosqldao.LopContent;
import org.eclipse.ecsp.nosqldao.Operator;
import org.eclipse.ecsp.nosqldao.Order;
import org.eclipse.ecsp.nosqldao.QueryTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * QueryTranslatorMorphia Implementation class.
 */
@SuppressWarnings("checkstyle:MatchXpath")
public class QueryTranslatorMorphiaImpl<E extends IgniteEntity> implements QueryTranslator<Query<E>> {

    /**
     * Entity class.
     */
    private Class<E> entityClass;

    /**
     * Datastore.
     */
    private Datastore datastore;

    /**
     * List of elemMatch filters.
     */
    private final ThreadLocal<List<Filter>> elemMatchFilterList = ThreadLocal.withInitial(ArrayList::new);

    /**
     * Find options.
     */
    private final ThreadLocal<FindOptions> findOptions = ThreadLocal.withInitial(FindOptions::new);

    /**
     * Default constructor.
     */
    public QueryTranslatorMorphiaImpl() {
    }

    /**
     * Constructor with parameters.
     *
     * @param datastore   : AdvancedDatastore
     * @param entityClass : Class
     */
    public QueryTranslatorMorphiaImpl(Datastore datastore, Class<E> entityClass) {
        this.entityClass = entityClass;
        this.datastore = datastore;
    }

    /**
     * Translate method.
     *
     * @param from           : IgniteQuery
     * @param collectionName : Optional
     * @return Query
     */
    @SuppressWarnings("deprecation")
    @Override
    public Query<E> translate(IgniteQuery from, Optional<String> collectionName) {
        Query<E> query = null;
        if (collectionName.isPresent()) {
            query = datastore.find(collectionName.get(), entityClass).disableValidation();
        } else {
            query = datastore.createQuery(entityClass).disableValidation();
        }
        elemMatchFilterList.get().clear();
        List<IgniteCriteriaGroup> igniteCriteriaGroups = from.getCriteriaGroups();
        LopContent lopContent = from.getLopContent();
        if (LopContent.MIXED.equals(lopContent)) {
            return getQueryForMixedLopContent(from, query);
        } else if (LopContent.ORONLY.equals(lopContent)) {
            createQueryForOronlyLopContent(query, igniteCriteriaGroups);
        } else if (LopContent.ANDONLY.equals(lopContent)) {
            createQueryForAndonlyLopContent(query, igniteCriteriaGroups);
        } else {
            Filter criteriaContainerFilter = createCriteriaContainer(igniteCriteriaGroups.get(0));
            elemMatchFilterList.get().add(criteriaContainerFilter);
            query.filter(criteriaContainerFilter);
        }

        // Projection
        FindOptions options = new FindOptions();

        if (from.getFieldNames().length != 0) {
            for (String fieldName : from.getFieldNames()) {
                options.projection().include(fieldName);
            }
        }
        if (from.getReadPreference() != null) {
            options.readPreference(from.getReadPreference());
        }
        applySort(from, options);
        setFindOptions(options);
        return query;
    }

    /**
     * Create Query for AND only LopContent.
     *
     * @param query                : Query
     * @param igniteCriteriaGroups : List
     */
    private void createQueryForAndonlyLopContent(Query<E> query, List<IgniteCriteriaGroup> igniteCriteriaGroups) {
        List<Filter> criteriaContainers = new ArrayList<>();
        for (IgniteCriteriaGroup igniteCriteriaGroup : igniteCriteriaGroups) {
            criteriaContainers.add(createCriteriaContainer(igniteCriteriaGroup));
        }
        elemMatchFilterList.get().add(Filters.or(criteriaContainers.toArray(new Filter[igniteCriteriaGroups.size()])));
        query.filter(Filters.and(
                criteriaContainers.toArray(new Filter[igniteCriteriaGroups.size()])));
    }

    /**
     * Create Query for OR only LopContent.
     *
     * @param query                : Query
     * @param igniteCriteriaGroups : List
     */
    private void createQueryForOronlyLopContent(Query<E> query, List<IgniteCriteriaGroup> igniteCriteriaGroups) {
        List<Filter> criteriaContainers = new ArrayList<>();
        for (IgniteCriteriaGroup igniteCriteriaGroup : igniteCriteriaGroups) {
            criteriaContainers.add(createCriteriaContainer(igniteCriteriaGroup));
        }
        elemMatchFilterList.get().add(Filters.or(criteriaContainers.toArray(new Filter[igniteCriteriaGroups.size()])));
        query.filter(Filters.or(
                criteriaContainers.toArray(new Filter[igniteCriteriaGroups.size()])));
    }

    /**
     * Get Query for Mixed LopContent.
     *
     * @param from  : IgniteQuery
     * @param query : Query
     * @return Query
     */
    private Query<E> getQueryForMixedLopContent(IgniteQuery from, Query<E> query) {
        List<List<IgniteCriteriaGroup>> groupedByOr = groupByOr(from);
        List<Filter> criteriaList = new ArrayList<>();
        for (List<IgniteCriteriaGroup> criteriaGroups : groupedByOr) {
            if (criteriaGroups.size() == 1) {
                criteriaList.add(createCriteriaContainer(criteriaGroups.get(0)));
            } else {
                List<Filter> andCriteriaList = new ArrayList<>();
                for (IgniteCriteriaGroup criteriaGroup : criteriaGroups) {
                    andCriteriaList.add(createCriteriaContainer(criteriaGroup));
                }
                criteriaList.add(Filters.and(andCriteriaList.toArray(new Filter[andCriteriaList.size()])));
            }
        }
        elemMatchFilterList.get().add(Filters.or(criteriaList.toArray(new Filter[criteriaList.size()])));
        query.filter(Filters.or(criteriaList.toArray(new Filter[criteriaList.size()])));
        return query;
    }

    /**
     * Apply Sort.
     *
     * @param from        : IgniteQuery
     * @param findOptions : FindOptions
     */
    private void applySort(IgniteQuery from, FindOptions findOptions) {
        if (!from.getOrderBys().isEmpty()) {
            List<Sort> sorts = new ArrayList<>();
            from.getOrderBys().forEach(orderBy -> sorts.add(getSort(orderBy)));
            findOptions.sort(sorts.toArray(new Sort[sorts.size()]));
        }
    }

    /**
     * Get Sort.
     *
     * @param orderBy : IgniteOrderBy
     * @return Sort
     */
    private Sort getSort(IgniteOrderBy orderBy) {
        Sort sort = null;
        if (Order.ASC == orderBy.getOrder()) {
            sort = Sort.ascending(orderBy.getFieldName());
        } else if (Order.DESC == orderBy.getOrder()) {
            sort = Sort.descending(orderBy.getFieldName());
        }
        return sort;
    }

    /**
     * Group By OR.
     *
     * @param from : IgniteQuery
     * @return List
     */
    private List<List<IgniteCriteriaGroup>> groupByOr(IgniteQuery from) {
        List<IgniteCriteriaGroup> criteriaGroups = from.getCriteriaGroups();
        List<LogicalOperator> operators = from.getIgniteQueryOperatorsList();
        List<List<IgniteCriteriaGroup>> groupedByOr = new ArrayList<>();
        List<IgniteCriteriaGroup> subGroup = new ArrayList<>();
        subGroup.add(criteriaGroups.get(0));
        int i = 1;
        for (LogicalOperator operator : operators) {
            if (LogicalOperator.OR.equals(operator)) {
                groupedByOr.add(subGroup);
                subGroup = new ArrayList<>();
                subGroup.add(criteriaGroups.get(i));
            } else {
                subGroup.add(criteriaGroups.get(i));
            }
            i++;
        }
        groupedByOr.add(subGroup);
        return groupedByOr;
    }

    /**
     * Group By OR.
     *
     * @param igniteCriteriaGroup : IgniteCriteriaGroup
     * @return List
     */
    private List<List<IgniteCriteria>> groupByOr(IgniteCriteriaGroup igniteCriteriaGroup) {
        List<IgniteCriteria> criterias = igniteCriteriaGroup.getCriterias();
        List<LogicalOperator> operators = igniteCriteriaGroup.getIgniteCriteriaGroupOperatorsList();
        List<List<IgniteCriteria>> groupedByOr = new ArrayList<>();
        List<IgniteCriteria> subGroup = new ArrayList<>();
        subGroup.add(criterias.get(0));
        int i = 1;
        for (LogicalOperator operator : operators) {
            if (LogicalOperator.OR.equals(operator)) {
                groupedByOr.add(subGroup);
                subGroup = new ArrayList<>();
                subGroup.add(criterias.get(i));
            } else {
                subGroup.add(criterias.get(i));
            }
            i++;
        }
        groupedByOr.add(subGroup);
        return groupedByOr;
    }

    /**
     * Create Criteria Container.
     *
     * @param igniteCriteriaGroup : IgniteCriteriaGroup
     * @return Filter
     */
    private Filter createCriteriaContainer(IgniteCriteriaGroup igniteCriteriaGroup) {
        List<IgniteCriteria> igniteCriterias = igniteCriteriaGroup.getCriterias();
        LopContent lopContent = igniteCriteriaGroup.getLopContent();
        if (LopContent.MIXED.equals(lopContent)) {
            return createCriteriaForLopContent(igniteCriteriaGroup);
        } else if (LopContent.ORONLY.equals(lopContent)) {
            List<Filter> filters = new ArrayList<>();
            for (IgniteCriteria igniteCriteria : igniteCriterias) {
                filters.add(createCriteria(igniteCriteria));
            }
            return createCriteriaContainer(LogicalOperator.OR, filters.toArray(new Filter[igniteCriterias.size()]));
        } else if (LopContent.ANDONLY.equals(lopContent)) {
            List<Filter> criterias = new ArrayList<>();
            for (IgniteCriteria igniteCriteria : igniteCriterias) {
                criterias.add(createCriteria(igniteCriteria));
            }
            return createCriteriaContainer(LogicalOperator.AND, criterias.toArray(new Filter[igniteCriterias.size()]));
        } else {
            return createCriteria(igniteCriterias.get(0));
        }
    }

    /**
     * Create Criteria Container.
     *
     * @param lop     : LogicalOperator
     * @param filters : Filter
     * @return Filter
     */
    private Filter createCriteriaContainer(LogicalOperator lop, Filter... filters) {
        Filter container = null;
        switch (lop) {
            case AND:
                container = Filters.and(filters);
                break;
            case OR:
                container = Filters.or(filters);
                break;
            default:
                throw new IllegalArgumentException("Invalid Logical Operator");
        }
        return container;
    }

    /**
     * Create Criteria for LopContent.
     *
     * @param igniteCriteriaGroup : IgniteCriteriaGroup
     * @return Filter
     */
    private Filter createCriteriaForLopContent(IgniteCriteriaGroup igniteCriteriaGroup) {
        List<List<IgniteCriteria>> groupedByOr = groupByOr(igniteCriteriaGroup);
        List<Filter> criteriaList = new ArrayList<>();
        for (List<IgniteCriteria> criteriaGroups : groupedByOr) {
            if (criteriaGroups.size() == 1) {
                criteriaList.add(createCriteria(criteriaGroups.get(0)));
            } else {
                List<Filter> andContainer = new ArrayList<>();
                for (IgniteCriteria igniteCriteria : criteriaGroups) {
                    andContainer.add(createCriteria(igniteCriteria));
                }
                criteriaList.add(Filters.and(andContainer.toArray(new Filter[andContainer.size()])));
            }
        }

        return Filters.or(criteriaList.toArray(new Filter[criteriaList.size()]));
    }

    /**
     * Create Criteria.
     *
     * @param igniteCriteria : IgniteCriteria
     * @return Filter
     */
    @SuppressWarnings({"checkstyle:MethodLength", "removal", "deprecation"})
    private Filter createCriteria(IgniteCriteria igniteCriteria) {
        Filter criteria = null;
        String field = igniteCriteria.getField();
        Operator operator = igniteCriteria.getOp();
        Object val = igniteCriteria.getVal();
        switch (operator) {
            case EQ:
                criteria = Filters.eq(field, val);
                break;
            case EQI:
                criteria = Filters.eq(field, Pattern.compile("^"
                        + Pattern.quote(val.toString()) + "$", Pattern.CASE_INSENSITIVE));
                break;
            case LT:
                criteria = Filters.lt(field, val);
                break;
            case LTE:
                criteria = Filters.lte(field, val);
                break;
            case GT:
                criteria = Filters.gt(field, val);
                break;
            case GTE:
                criteria = Filters.gte(field, val);
                break;
            case CONTAINS:
                criteria = Filters.eq(field, Pattern.compile(Pattern.quote((String) val)));
                break;
            case CONTAINS_IGNORE_CASE:
                criteria = Filters.eq(field, Pattern.compile(Pattern.quote((String) val),
                        Pattern.CASE_INSENSITIVE));
                break;
            case IN:
                criteria = Filters.in(field, (Iterable<?>) val);
                break;
            case NOT_IN:
                criteria = Filters.nin(field, val);
                break;
            case NEQ:
                criteria = Filters.ne(field, val);
                break;
            case ELEMENT_MATCH:
                // prepare morphia filter list
                translate((IgniteQuery) val, Optional.empty());
                criteria = Filters.elemMatch(field, elemMatchFilterList.get().toArray(
                        new Filter[elemMatchFilterList.get().size()]));
                break;
            case NEAR:
                if (val instanceof Coordinate coordinates) {
                    Point geoPoint = new Point(new Position(coordinates.getLongitude(), coordinates.getLatitude()));
                    criteria = Filters.nearSphere(field, geoPoint).maxDistance(coordinates.getRadius())
                            .minDistance(0.0);
                    break;
                } else {
                    throw new IllegalArgumentException("Value is not an instance of Coordinates");
                }

            default:
                throw new IllegalArgumentException("Invalid Operator");
        }
        return criteria;
    }

    /**
     * Get Find Options.
     *
     * @return FindOptions : FindOptions
     */
    public FindOptions getFindOptions() {
        FindOptions options = findOptions.get();
        return options != null ? options : new FindOptions();
    }

    /**
     * Set Find Options.
     *
     * @param findOptions : FindOptions
     */
    private void setFindOptions(FindOptions findOptions) {
        this.findOptions.set(findOptions);
    }
}
