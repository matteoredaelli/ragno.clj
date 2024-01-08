HOME = /home/matteo/Downloads
RAGNO_DATA ?= ${HOME}/ragnocli

RAW_FILES = $(wildcard ${RAGNO_DATA}/*.raw)
PARSED_FILES = ${RAW_FILES:.raw=.parsed}
VISITED_FILES = ${RAW_FILES:.raw=.tmp.visited}
REDIRECTS_FILES = ${RAW_FILES:.raw=.tmp.redirects}
LINKED_FILES = ${RAW_FILES:.raw=.tmp.linked}
TODO_FILES = $(wildcard ${RAGNO_DATA}/*.todo)
DONE_FILES = ${TODO_FILES:.todo=.done}

TS=$(shell date +"%Y%m%d-%H%M%S-$$$$")

help:
	echo ${RAW_FILES} ${VISITED_FILES}

clean-tmp:
	@rm -f ${RAGNO_DATA}/*.tmp.*

clean:
	@rm -f ${RAGNO_DATA}/*.tmp.*  ${RAGNO_DATA}/*.todo ${RAGNO_DATA}/redirects ${RAGNO_DATA}/linked ${RAGNO_DATA}/visited

setup:
	mkdir -p ${RAGNO_DATA}
	touch  ${RAGNO_DATA}/visited ${RAGNO_DATA}/redirects  ${RAGNO_DATA}/redirects

reset: clean setup

%.parsed: %.raw
	cat $< | jq -r '.url' | sort -u >  ${@:.parsed=.tmp.visited}
	cat $< |  jq -r '[."final-domain", .status] | @csv' | grep 301 | cut -f1 -d',' | sed 's/"//g' | sort -u > ${@:.parsed=.tmp.redirects}
	cat $< | jq -r '."domain-links"[]?' | sort -u > ${@:.parsed=.tmp.linked}
	mv $< $@

visited: ${PARSED_FILES}
	cat ${RAGNO_DATA}/*.tmp.visited ${RAGNO_DATA}/${@} | sort -u >  ${RAGNO_DATA}/${@}.tmp
	mv ${RAGNO_DATA}/${@} ${RAGNO_DATA}/${@}.old
	mv ${RAGNO_DATA}/${@}.tmp ${RAGNO_DATA}/${@}

redirects: ${PARSED_FILES}
	cat ${RAGNO_DATA}/*.tmp.redirects  | sort -u >  ${RAGNO_DATA}/$@

linked: ${PARSED_FILES}
	cat ${RAGNO_DATA}/*.tmp.linked  | sort -u >  ${RAGNO_DATA}/$@

todo: visited redirects linked
## [X] considering only https: converting http links to https links
## [X] removing adult contents
## [X] removing www?.  (www2,..)
	cat ${RAGNO_DATA}/redirects ${RAGNO_DATA}/linked | grep -v porn | grep -v sex | grep -v xxx | sed -e 's/http:/https:/' | sed -r 's/www[0-9]?\.//'| egrep "^https?://(www\.)?[^.]+\.[^.]+$$" | fgrep -v -f ${RAGNO_DATA}/visited > ${RAGNO_DATA}/${TS}.todo

%.done: %.todo
	clojure -X net.clojars.matteoredaelli.ragno/cli :urlfile \"$<\"  :config-file \"ragno.edn\" > ${<:.todo=.raw}
	@mv $< $@

run: ${DONE_FILES}
	@rm -f ${RAGNO_DATA}/*.tmp.*

