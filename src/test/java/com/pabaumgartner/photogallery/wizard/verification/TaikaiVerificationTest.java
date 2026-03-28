package com.pabaumgartner.photogallery.wizard.verification;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.STATIC;

import java.util.List;

import com.enofex.taikai.Taikai;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class TaikaiVerificationTest {

	@Test
	void should_satisfy_architecture_rules() {
		Taikai.builder()
			.namespace("com.pabaumgartner.photogallery.wizard")
			.java(java -> java.noUsageOf(java.util.Date.class)
				.noUsageOf(java.util.Calendar.class)
				.noUsageOfDeprecatedAPIs()
				.noUsageOfSystemOutOrErr()
				.classesShouldImplementHashCodeAndEquals()
				.utilityClassesShouldBeFinalAndHavePrivateConstructor()
				.fieldsShouldNotBePublic()
				.finalClassesShouldNotHaveProtectedMembers()
				.imports(imports -> imports.shouldHaveNoCycles())
				.naming(naming -> naming.packagesShouldMatchDefault()
					.classesShouldNotMatch(".*Impl")
					.constantsShouldFollowConventions()
					.interfacesShouldNotHavePrefixI()))
			.logging(logging -> logging.loggersShouldFollowConventions(Logger.class, "LOGGER",
					List.of(PRIVATE, STATIC, FINAL)))
			.test(test -> test.junit(junit -> junit.classesShouldNotBeAnnotatedWithDisabled()
				.methodsShouldNotBeAnnotatedWithDisabled()
				.classesShouldBePackagePrivate(".*Tests?")
				.methodsShouldBePackagePrivate()
				.methodsShouldContainAssertionsOrVerifications()))
			.spring(spring -> spring.noAutowiredFields()
				.boot(boot -> boot.applicationClassShouldResideInPackage("com.pabaumgartner.photogallery.wizard"))
				.properties(properties -> properties.namesShouldEndWithProperties()
					.shouldBeAnnotatedWithConfigurationProperties()
					.shouldBeRecords())
				.services(services -> services.namesShouldEndWithService()
					.shouldBeAnnotatedWithService()
					.shouldNotDependOnControllers()))
			.build()
			.check();
	}

}
