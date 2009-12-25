


cmapdump: cmapdump.c
	gcc -ocmapdump cmapdump.c -I../fitz


fonttump: fontdump.c
	gcc -ofontdump fontdump.c


font_misc.c: fontdump
	./fontdump font_misc.c \
		../fonts/Dingbats.cff \
		../fonts/StandardSymL.cff \
		../fonts/URWChanceryL-MediItal.cff
	

font_mono.c: fontdump
	./fontdump font_mono.c \
		../fonts/NimbusMonL-Regu.cff \
		../fonts/NimbusMonL-ReguObli.cff \
		../fonts/NimbusMonL-Bold.cff \
		../fonts/NimbusMonL-BoldObli.cff


font_sans.c: fontdump
	./fontdump font_sans.c \
		../fonts/NimbusSanL-Bold.cff \
		../fonts/NimbusSanL-BoldItal.cff \
		../fonts/NimbusSanL-Regu.cff \
		../fonts/NimbusSanL-ReguItal.cff


font_serif.c: fontdump
	./fontdump font_serif.c \
		../fonts/NimbusRomNo9L-Regu.cff \
		../fonts/NimbusRomNo9L-ReguItal.cff \
		../fonts/NimbusRomNo9L-Medi.cff \
		../fonts/NimbusRomNo9L-MediItal.cff



# vim: set sts=8 ts=8 sw=8 noet:
