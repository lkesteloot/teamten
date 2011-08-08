
WEBSITE=$(HOME)/Personal/website
WRITINGS=$(WEBSITE)/writings

.PHONY: all remote

all:	book.pdf cover.png

book.pdf: $(WRITINGS)/book.html $(WRITINGS)/book.css $(WEBSITE)/stylesheet.css \
	$(WRITINGS)/stylesheet.css $(WRITINGS)/coding-machines/stylesheet.css Makefile

	prince -v ~/Personal/website/writings/book.html -o book.pdf

cover.png: make_cover Makefile
	./make_cover cover.png

print.png: make_cover Makefile
	./make_cover --print print.png

remote:
	prince -v http://www.teamten.com/lawrence/writings/book.html -o book.pdf
