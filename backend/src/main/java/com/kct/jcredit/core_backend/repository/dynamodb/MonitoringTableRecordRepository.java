// package com.kct.jcredit.core_backend.repository.dynamodb;

// import org.springframework.stereotype.Repository;

// import com.kct.jcredit.core_backend.document.SolarMonitoringTableRecord;

// import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
// import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
// import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

// @Repository
// public class MonitoringTableRecordRepository {
//     private final DynamoDbTable<SolarMonitoringTableRecord> table;

//     public MonitoringTableRecordRepository(DynamoDbEnhancedClient enhancedClient) {
//         this.table = enhancedClient.table("solar_monitoring_table_record",
//                 TableSchema.fromBean(SolarMonitoringTableRecord.class));
//     }

//     public void save(SolarMonitoringTableRecord testdoc) {
//         table.putItem(testdoc);
//     }

//     public SolarMonitoringTableRecord findByPKAndSK(String pk, String sk) {
//         return table.getItem(r -> r.key(k -> k
//             .partitionValue(pk)
//             .sortValue(sk)));
//     }
// }
