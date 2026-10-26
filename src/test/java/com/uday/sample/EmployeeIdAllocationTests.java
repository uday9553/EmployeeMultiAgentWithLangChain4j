package com.uday.sample;

import com.uday.sample.model.Employee;
import com.uday.sample.repository.EmployeeRepository;
import com.uday.sample.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employee-id-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.sql.init.mode=never"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EmployeeIdAllocationTests {
    @Autowired EmployeeService service;
    @Autowired EmployeeRepository employees;
    @Autowired PlatformTransactionManager transactions;

    private Map<String, String> fields(String name) {
        return Map.of("name", name, "department", "Engineering", "salary", "25000", "designation", "UI Dev");
    }

    @Test
    void emptyDatabaseStartsAt1000AndPersistsZeroDefaults() {
        var created = service.create(fields("Meghana"));
        var saved = employees.findByEmployeeId(created.getEmployeeId()).orElseThrow();
        assertEquals("EMP-1000", saved.getEmployeeId());
        assertEquals(0, saved.getMonthsInRole());
        assertEquals(0, saved.getPerformanceRating());
        assertEquals("EMP-1001", service.create(fields("Next employee")).getEmployeeId());
    }

    @Test
    void concurrentCreatesContinueAboveExistingNumericIdsWithoutDuplicates() throws Exception {
        var existing = new Employee();
        existing.setEmployeeId("EMP-1040");
        employees.saveAndFlush(existing);
        var legacy = new Employee();
        legacy.setEmployeeId("EMP-" + UUID.randomUUID());
        employees.saveAndFlush(legacy);

        int count = 12;
        var ready = new CountDownLatch(count);
        var start = new CountDownLatch(1);
        Set<String> ids = new HashSet<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> pending = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                String name = "Concurrent " + index;
                pending.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    return service.create(fields(name)).getEmployeeId();
                }));
            }
            try {
                assertTrue(ready.await(10, TimeUnit.SECONDS));
            } finally {
                start.countDown();
            }
            for (var result : pending) assertTrue(ids.add(result.get(30, TimeUnit.SECONDS)), "Duplicate employee ID");
        }
        assertEquals(new HashSet<>(LongStream.range(1041, 1041 + count).mapToObj(n -> "EMP-" + n).toList()), ids);
        assertEquals(count + 2, employees.count());
        assertEquals("EMP-1053", service.create(fields("After concurrency")).getEmployeeId());
    }

    @Test
    void counterAndEmployeeRollBackTogether() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            assertEquals("EMP-1000", service.create(fields("Rolled back")).getEmployeeId());
            status.setRollbackOnly();
        });
        assertTrue(employees.findByEmployeeId("EMP-1000").isEmpty());
        assertEquals("EMP-1000", service.create(fields("Committed")).getEmployeeId());
    }
}
