# Quote redistribution review

Reviewed 26 October 2026. **Publication blocker: the complete anthology is not cleared for public redistribution.** This is a rights triage, not a legal clearance opinion. The repository must remain private pending the owner's decision and a defensible rights basis for every retained passage.

The MIT licence applies to original app code, not quotations, translations, publisher excerpts, or third-party notices. A source URL, an attribution, a short quotation, a publicly viewable publisher sample, or an original editorial translation does not by itself grant redistribution rights.

## Findings

The bundled JSON has 1,643 entries from 95 attributed thinkers. All entries have been inventoried in [docs/quote-rights-review.csv](docs/quote-rights-review.csv), including work, edition/translator and source URL. No entry is marked cleared merely because it is old or appears on Project Gutenberg.

- **51 modern-work entries need priority review:** Albert Camus, Arnold J. Toynbee, Arthur C. Clarke, Ayn Rand, Carl Schmitt, Emil Cioran, Ernst Jünger, Friedrich Hayek, Karl Jaspers, Karl Popper, Leo Strauss, Ludwig von Mises, Martin Heidegger, Milton Friedman, Nick Land, Nicolae Steinhardt, Slavoj Žižek. No explicit redistribution permission for these entries was established in this audit. Publisher/foundation excerpts and classroom reprints are not sublicences to this app.
- **42 entries need priority translation, edition or territorial review:** Demosthenes, Fernando Pessoa, Franz Kafka, G. W. F. Hegel, George Orwell, Giambattista Vico, Isocrates, Jean-Jacques Rousseau, Max Weber, Oswald Spengler, Scipio Africanus, Stendhal, Søren Kierkegaard, Thomas Aquinas. These flags do not assert that every named text is copyrighted everywhere. Examples include David Wyllie's Kafka translation, Gerth/Mills and Hurst adaptations of Weber, Hollander's Kierkegaard, and modern editorial translations of underlying works.
- The remaining historical-source entries still need work/translation/edition-specific checking. An ancient author does not make a recent translation public domain; US and UK status can differ. Unnamed translators require further identification.
- The existing Project Gutenberg notice and liquid-glass MIT notice are retained. The Gutenberg notice is not a blanket licence for material sourced from other publishers.

## Basis for caution

[Project Gutenberg's permission guidance](https://www.gutenberg.org/policy/permission) distinguishes US public-domain works from copyrighted entries, and says non-US users must check applicable rights. Its [terms](https://www.gutenberg.org/policy/terms_of_use.html) do not guarantee public-domain status outside the US.

[UK National Archives copyright guidance](https://www.nationalarchives.gov.uk/terms-and-conditions/copyright/copyright-and-related-rights/) explains the usual life-plus-70 term and exceptions. A blanket word-count threshold is not a permission test. Attribution and a reading note do not automatically establish fair dealing for an entire redistributable quotation database.

## Before any public repository or release

For each entry, record a verified public-domain basis for relevant territories, an explicit compatible licence, documented permission, or a specifically reviewed quotation-exception basis. Otherwise exclude the text from the public source, including copies in history, scripts and editorial files. Citation metadata alone can be retained where appropriate. A reduced, verified historical collection is the conservative route; it would change the published quote count and requires corresponding README/app/demo copy changes.

Do not merely delete uncertain texts in a later commit and then make this repository public: old commits still contain them. Prepare a clean public history after the retained corpus is approved. The private source snapshot includes the current anthology for review; it is not permission to publish that anthology.
