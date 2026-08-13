package com.kct.jcredit.core_backend.repository.dynamodb;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormEnvelope;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanFormRegistry;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

/**
 * Single DynamoDB gateway for every project-plan page, replacing the
 * per-form repositories that differed only in a type parameter.
 *
 * <p>All pages of a project live in one partition ({@code PK = projectId}) with
 * the page as sort key, so a page save can never clobber another page.
 */
@Repository
public class ProjectPlanFormRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final String tableName;
    private final ProjectPlanFormRegistry registry;
    private final DynamoDbTable<ProjectPlanFormEnvelope> envelopeTable;

    /** {@code TableSchema.fromBean} reflects over the bean; build it once per type. */
    private final Map<Class<?>, DynamoDbTable<?>> tables = new ConcurrentHashMap<>();

    public ProjectPlanFormRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${app.dynamodb.table.project-plan}") String tableName,
            ProjectPlanFormRegistry registry) {
        this.enhancedClient = enhancedClient;
        this.tableName = tableName;
        this.registry = registry;
        this.envelopeTable =
                enhancedClient.table(tableName, TableSchema.fromBean(ProjectPlanFormEnvelope.class));
    }

    /** Warms every registered page's schema so no request pays the reflection cost. */
    @PostConstruct
    void warmSchemas() {
        registry.all().forEach(definition -> tableFor(definition.type()));
    }

    @SuppressWarnings("unchecked")
    private <T> DynamoDbTable<T> tableFor(Class<T> beanClass) {
        return (DynamoDbTable<T>) tables.computeIfAbsent(
                beanClass, type -> enhancedClient.table(tableName, TableSchema.fromBean(type)));
    }

    public <T extends ProjectPlanFormBase> Optional<T> find(Class<T> type, String pk, String sk) {
        return Optional.ofNullable(
                tableFor(type).getItem(Key.builder().partitionValue(pk).sortValue(sk).build()));
    }

    public <T extends ProjectPlanFormBase> T save(Class<T> type, T item) {
        tableFor(type).putItem(item);
        return item;
    }

    /**
     * Metadata for every stored page of a project, in one Query.
     *
     * <p>Reads heterogeneous items through the narrow envelope schema; the
     * enhanced client silently drops attributes the schema does not map. Items
     * whose attributes match nothing map to {@code null}, hence the filter.
     */
    public List<ProjectPlanFormEnvelope> findEnvelopes(String pk) {
        return envelopeTable
                .query(request -> request.queryConditional(
                        QueryConditional.keyEqualTo(Key.builder().partitionValue(pk).build())))
                .items()
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
