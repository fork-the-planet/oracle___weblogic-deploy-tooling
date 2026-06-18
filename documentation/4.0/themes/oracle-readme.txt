The current vendored Relearn theme is hugo-theme-relearn-9.0.3.

Do not edit files inside the vendored Relearn theme.  WebLogic Deploy Tooling customizations live in these
site-level override files:

  documentation/4.0/layouts/partials/custom-header.html
  documentation/4.0/layouts/partials/heading.html
  documentation/4.0/layouts/partials/logo.html
  documentation/4.0/layouts/partials/menu-footer.html

The old theme-local external link render hook was intentionally not carried forward.  The site now uses the
Relearn parameter externalLinkTarget = "_blank" in documentation/4.0/config.toml.

When installing a new theme or even a new version of the Relearn theme, edit documentation/4.0/config.toml
to set the theme line to point to the new directory, review the override files listed above, and build the
site with the Hugo version used by CI.  Relearn 9.0.3 has been validated with Hugo 0.161.1.

Useful validation commands:

  hugo -s documentation/4.0 -d /private/tmp/wdt-docs-build
  documentation/4.0/runlocal.sh
