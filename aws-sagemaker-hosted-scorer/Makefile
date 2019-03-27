PROPERTIES_DIR = src/main/resources

default: build

clean:
	./gradlew clean
	rm -f $(PROPERTIES_DIR)/version.properties

build:
	$(MAKE) clean
	./gradlew build

.PHONY: clean build run

