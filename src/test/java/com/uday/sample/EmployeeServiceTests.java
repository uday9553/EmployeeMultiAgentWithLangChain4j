package com.uday.sample;

import com.uday.sample.model.Employee;
import com.uday.sample.repository.EmployeeRepository;
import com.uday.sample.service.EmployeeService;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeServiceTests {
    @Test
    void validatesCreateAndUpdate() {
        var repository = mock(EmployeeRepository.class);
        var ids = mock(com.uday.sample.service.EmployeeIdAllocator.class);
        when(ids.nextId()).thenReturn("EMP-1006");
        var service = new EmployeeService(repository, ids);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var employee = service.create(Map.of("name", "Rahul", "department", "Engineering", "salary", "60000"));
        assertEquals("EMP-1006", employee.getEmployeeId());
        assertEquals(0, employee.getMonthsInRole());
        assertEquals(0, employee.getPerformanceRating());
        assertEquals("Engineering", employee.getDepartment());
        when(repository.findByEmployeeId(employee.getEmployeeId())).thenReturn(Optional.of(employee));
        assertEquals("Finance", service.update(employee.getEmployeeId(), Map.of("department", "Finance")).getDepartment());
        assertThrows(IllegalArgumentException.class, () -> service.update(employee.getEmployeeId(), Map.of("salary", "-1")));
        assertThrows(IllegalArgumentException.class, () -> service.create(Map.of("name", "Only name")));
        assertThrows(IllegalArgumentException.class, () -> service.update(employee.getEmployeeId(), Map.of("performanceRating", "5")));
    }
}
