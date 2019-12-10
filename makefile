PLANTUML_JAR_URL = https://sourceforge.net/projects/plantuml/files/plantuml.jar/download
DIAGRAMS_SRC := $(wildcard  docs/diagrams/*.puml)
DIAGRAMS_PNG := $(addsuffix .png, $(basename $(DIAGRAMS_SRC)))

# Default target first; build PNGs, probably what we want most of the time
png: plantuml.jar $(DIAGRAMS_PNG)

# clean up compiled files
clean:
	rm -f plantuml.jar $(DIAGRAMS_PNG) $(DIAGRAMS_SVG)

# If the JAR file isn't already present, download it
plantuml.jar:
	curl -sSfL $(PLANTUML_JAR_URL) -o plantuml.jar

docs/diagrams/%.png: docs/diagrams/%.puml
	java -jar plantuml.jar -tpng $^

# Quirk of GNU Make: https://www.gnu.org/software/make/manual/html_node/Phony-Targets.html
.PHONY: png clean