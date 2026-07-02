package com.orbitastra.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.core.SchoolRepository;

@SpringBootTest
class BackendApplicationTests {

	@Autowired
	private SchoolRepository schoolRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void cleanDuplicateSubdomains() {
		List<School> schools = schoolRepository.findAll();
		System.out.println("==================================================");
		System.out.println("TOTAL SCHOOLS IN DB: " + schools.size());
		
		Map<String, List<School>> grouped = schools.stream()
				.filter(s -> s.getSubdomain() != null)
				.collect(Collectors.groupingBy(School::getSubdomain));
				
		for (Map.Entry<String, List<School>> entry : grouped.entrySet()) {
			List<School> list = entry.getValue();
			if (list.size() > 1) {
				System.out.println("Duplicate subdomain found: " + entry.getKey() + " (" + list.size() + " times)");
				// Keep the first one, delete the rest
				for (int i = 1; i < list.size(); i++) {
					School toDelete = list.get(i);
					System.out.println("DELETING duplicate school ID: " + toDelete.getId() + " Name: " + toDelete.getSchoolName());
					schoolRepository.delete(toDelete);
				}
			}
		}
		System.out.println("==================================================");
	}
}
