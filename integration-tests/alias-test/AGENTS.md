# Alias Test Guidance

## Alias Test Verify

Run from `integration-tests/alias-test/verify`.

Use generated-file download by default:

`mvn -B verify -DskipTests=true -Dalias-test-skipITs=false -Dskip-wdt-install=true -Dalias_test_tenancy=<oci_namespace> -Dalias_test_oci_profile=<oci_profile> -Dwls_version=<version>`

Do not pass `-Dskip-generated-file-download=true` unless explicitly asked to verify existing local `target` files.

If `WKT_TENANCY` or `WKT_DEFAULT_OCI_PROFILE` are unset, discover a working local OCI profile without printing config contents:

`awk '/^\[.*\]$/ { print }' ~/.oci/config`

Then test likely profiles with:

`oci os ns get --profile <profile> --query data --raw-output`

Use the returned namespace as `alias_test_tenancy` and the profile name as `alias_test_oci_profile`.

Maven may need approval/escalation because `oci-maven-plugin` writes under `~/.m2/caches/kordamp`.

After the run, summarize `target/reportOnline-<version>.txt` and `target/reportOffline-<version>.txt`.

## Alias Curly-Brace Values

When fixing alias verify failures, remember that alias string fields can use curly-brace mode substitutions:

`${offline_value:online_value}`

Examples:

- `get_method: "${LSA:GET}"` means offline uses `LSA`, online uses `GET`.
- `wlst_name: "CandidateMachine${:s}"` means offline uses `CandidateMachine`, online uses `CandidateMachines`.
- `${__NULL__:value}` means offline resolves to `null`, online resolves to `value`.

Use this for small offline/online value differences in fields like `wlst_name`, `default_value`, `get_method`, `set_method`, `wlst_type`, and paths. Do not use curly braces in `wlst_mode`; split alias entries if mode availability or version ranges differ.
