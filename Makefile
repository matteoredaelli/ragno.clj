HOME = /home/matteo/Downloads
RAGNO_DATA ?= ${HOME}/ragnocli

RAW_FILES = $(wildcard ${RAGNO_DATA}/*.raw)
PARSED_FILES = ${RAW_FILES:.raw=.parsed}
VISITED_FILES = ${RAW_FILES:.raw=.tmp.visited}
REDIRECTS_FILES = ${RAW_FILES:.raw=.tmp.redirects}
LINKED_FILES = ${RAW_FILES:.raw=.tmp.linked}
TODO_FILES = $(wildcard ${RAGNO_DATA}/*.todo)
TODOSPLIT_FILES = $(wildcard ${RAGNO_DATA}/*.todo.split)
DONE_FILES = ${TODOSPLIT_FILES:.todo.split=.done}

TS=$(shell date +"%Y%m%d-%H%M-$$PPID")

help:
	echo ${RAW_FILES} ${VISITED_FILES}

testts:
	echo ${TS}
	echo ${TS}

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
	cat ${RAGNO_DATA}/redirects ${RAGNO_DATA}/linked | grep -v porn | grep -v adult | grep -v sex | grep -v xxx | sed -e 's/http:/https:/' | sed -r 's/www[0-9]?\.//'| egrep "^https?://(www\.)?[^.]+\.[^.]+$$" | tr '[:upper:]' '[:lower:]' | grep -v ru$$ | sort -u | fgrep -v -f ${RAGNO_DATA}/visited > ${RAGNO_DATA}/${TS}.todo
	split -l500 --additional-suffix=.todo.split ${RAGNO_DATA}/${TS}.todo ${RAGNO_DATA}/${TS}_
	mv  ${RAGNO_DATA}/${TS}.todo  ${RAGNO_DATA}/${TS}.done

%.done: %.todo.split
	clojure -X net.clojars.matteoredaelli.ragno/cli-pmap :urlfile \"$<\"  :config-file \"ragno.edn\" > ${<:.todo.split=.raw}
	@mv $< $@

run: ${DONE_FILES}
	@rm -f ${RAGNO_DATA}/*.tmp.*

tags:
	cat ${RAGNO_DATA}/*.parsed |jq -r '.tags?[]?' | sort -u > ${RAGNO_DATA}/tags
	cat ${RAGNO_DATA}/*.parsed ${RAGNO_DATA}/*.raw |jq -r '[.url, .status, ."tags"?[]?] |@tsv'|  grep -v "\-1$$" | sort -u > ${RAGNO_DATA}/domains_with_tags

domains-by-tag: tags
	cat ${RAGNO_DATA}/tags | while read tag ; do echo TAG $tag ; grep $tag domains-with-tags | cut -f1 > ${RAGNO_DATA}/domains-by-tag-${tag}; done

info: tags
	@echo Visited sites:
	@wc -l  ${RAGNO_DATA}/domains_with_tags | cut -f1 -d' '
	@echo TOP level1 domains:
	@cat ${RAGNO_DATA}/domains_with_tags | cut -f1 | cut -f2 -d'.'| cut -f1 -d':' |  sort | uniq -c | sort -n -r | head -20
	@echo TOP TAGS
	@wc -l  ${RAGNO_DATA}/domains-tag-* | sort -nr | grep -v "total$$"
