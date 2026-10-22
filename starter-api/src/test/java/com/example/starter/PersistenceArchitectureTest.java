package com.example.starter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class PersistenceArchitectureTest {
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.example.starter");

    @Test
    void persistenceDoesNotDependOnTheClient() {
        noClasses()
                .that()
                .resideInAPackage("..adapter.persistence..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..adapter.client..")
                .check(CLASSES);
    }

    @Test
    void clientDoesNotDependOnPersistence() {
        noClasses()
                .that()
                .resideInAPackage("..adapter.client..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..adapter.persistence..")
                .check(CLASSES);
    }
}
