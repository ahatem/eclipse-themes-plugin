# Design

## Context

See [proposal.md](proposal.md#why) for motivation and [the parsing delta](specs/theme-parsing/spec.md) for the contract.

**Decision Gate status:** the maintainer explicitly approved [PM1 Option A in the proposal](proposal.md#decision-gate) on 2026-10-13: reject every DOCTYPE, including empty and internal-only DTDs. The documented loss of DTD-dependent legacy themes and the absence of a representative non-bundled corpus are accepted; universal legacy compatibility must not be inferred from bundled themes. This design and its implementation approach remain unchanged. The later apply request authorized implementation; ED1/ED3 are resolved through the planned executed checks in [verification.md](verification.md). The earlier approval-record request stopped before implementation.

### Current-code re-verification (2026-10-13)

The planning checkout is clean before this change, on `fix/xml-parser-security` at `3d303ee`; local `develop` and `origin/develop` both point to `f6e4ca9`. The extra commit archives the completed preference-reset change. No parser changes appear in the diff from `develop`, and `openspec list --json` reports no active changes before creation. The roadmap's older baseline is not treated as current evidence.

- [`XmlThemeParser.parse(InputStream, File)`](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/parser/XmlThemeParser.java), lines 28-32, calls `DocumentBuilderFactory.newInstance()`, `newDocumentBuilder()`, then `parse(inputStream)` with no security features, access restrictions, or entity resolver. Its final `catch (Exception)` wraps failures with a generic message; factory-discovery `FactoryConfigurationError` is outside that catch.
- [`ThemeParser`](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/parser/ThemeParser.java) delegates both convenience entry points to that method. [`ParserFactory`](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/core/parser/ParserFactory.java) shares a stateless XML parser; it does not share a DOM builder. [`ThemeRepository`](../../../bundles/com.github.eclipsethemes/src/com/github/eclipsethemes/eclipse/ThemeRepository.java) uses it for bundled loading, installed loading, and imports. There is no separate trusted-source path to harden.
- All 33 current bundled XML files were searched: none declares DOCTYPE/entities, XInclude, or schema locations. Metadata extraction, aliases, color/style handling, and fallback policy execute after DOM parsing; this design leaves that code unchanged.
- The runtime bundle already has JUnit 4.13.2, a `test/` source tree, and Surefire execution. Q16's historical absence of tests is no longer true. The parser still calls the activator on warning paths; that A2 coupling is not needed for rejecting DTD input and is not refactored here.
- Q2's original audit records a production-parser probe reading a harmless local XML file and using its background color. This planning pass establishes the defect by direct code evidence; it does not claim a new executed exploit reproduction, network disclosure, or resource-exhaustion reproduction.

The accepted specs define successful legacy parsing and observable failures, but defer security hardening. Only `theme-parsing` needs a delta. R1's separate structural-validation requirement and R2's import contracts are not promoted here.

## Goals / Non-Goals

**Goals:** make each DOM builder secure before reading input; use one auditable configuration path for all parser entry points; make unsupported controls deterministically testable; preserve legacy outputs for documents that meet the security policy.

**Non-Goals:** XML schema/root/metadata/token validation, new diagnostics models, alias cleanup, parser/activator separation, repository rewrites, import transaction safety, threading changes, custom file-size/depth quotas, and a general test-framework expansion. Secure-processing defaults are defense in depth, not a promise to bound arbitrary large DTD-free documents.

## Decisions

### 1. Retain DOM and provider discovery; configure every required control explicitly

Keep `DocumentBuilderFactory.newInstance()` and a fresh factory/builder per parse. Apply the following controls in one small parser-local helper, before constructing the builder, and read back their effective values. A setter exception, unavailable getter, or mismatch is a configuration failure. Keep validation disabled and do not attach a schema; keep existing namespace interpretation.

| Control | Required value |
| --- | --- |
| `XMLConstants.FEATURE_SECURE_PROCESSING` | `true` |
| `http://apache.org/xml/features/disallow-doctype-decl` | `true` |
| `http://xml.org/sax/features/external-general-entities` | `false` |
| `http://xml.org/sax/features/external-parameter-entities` | `false` |
| `http://apache.org/xml/features/nonvalidating/load-external-dtd` | `false` |
| `XMLConstants.ACCESS_EXTERNAL_DTD` | empty protocol allowlist |
| `XMLConstants.ACCESS_EXTERNAL_SCHEMA` | empty protocol allowlist |
| XInclude awareness | `false` |
| Entity-reference expansion | `false` |

Use public JAXP methods and constants. The Apache/SAX feature URIs are provider-specific controls, not portable guarantees: isolate them in this helper and verify them on the Java 17 JDK provider and the actual Eclipse build/runtime provider. Reject providers lacking support. Do not import Xerces or JDK implementation classes, mutate JVM system properties, or change OSGi provider/class-loader selection.

The [Java 17 factory API](https://docs.oracle.com/en/java/javase/17/docs/api/java.xml/javax/xml/parsers/DocumentBuilderFactory.html) defines configuration and readback methods; [XMLConstants](https://docs.oracle.com/en/java/javase/17/docs/api/java.xml/javax/xml/XMLConstants.html) defines the standard secure-processing and external-access controls. [JAXP security guidance](https://docs.oracle.com/en/java/javase/17/security/java-api-xml-processing-jaxp-security-guide.html) supports explicit factory-local restrictions rather than reliance on ambient defaults.

Alternatives considered: switching to SAX/StAX would disturb existing DOM interpretation; pinning the built-in provider would change discovery compatibility; relying only on secure-processing defaults or disabling entity expansion would not establish the complete required policy. Raw-text DOCTYPE scanning is also rejected because XML encodings and syntax must be handled by the XML processor.

### 2. Reject all DTDs, with an external-resolution guard as defense in depth

This is approved PM1 Option A. The following approach remains unchanged; Option B was not selected and is not part of the implementation plan.

DOCTYPE rejection is unconditional, including empty/internal-only declarations. Disabled general/parameter entities and external-DTD loading complement that rule. After creating the configured builder, attach a rejecting `EntityResolver2` whose external-subset and resolution callbacks throw `SAXException` without opening identifiers, returning streams, or supplying an empty document. Attach a throwing SAX error handler so rejected documents produce exceptions through the caller contract rather than only default stderr output. Check builder-reported validation/XInclude state before parsing where exposed.

XInclude is disabled rather than newly validated. XInclude/schema-location syntax alone has no new structural rejection rule; the contract is that it cannot cause retrieval or incorporation. Unknown-element handling continues to follow the existing parser. Standard escapes and numeric references remain valid without a DTD.

Alternatives considered: returning an empty resolved source could fabricate a successful incomplete theme; allowing internal DTDs would retain entity expansion/defaulting behavior outside the supported legacy needs. The resolver guard alone is insufficient because it does not itself reject all internal declarations.

### 3. Finish security setup before consuming input and preserve useful failure causes

Ordering is: discover factory, set and verify controls, create builder, install resolver/error handler, confirm exposed builder state, parse DOM, interpret theme. On any setup failure, abort before `builder.parse` and before reading the input. No retries with fewer controls and no reuse of a partially configured builder.

Translate factory-discovery `FactoryConfigurationError` specifically, plus configuration exceptions such as `ParserConfigurationException`, `IllegalArgumentException`, and `UnsupportedOperationException`, to `ThemeParseException` with the failing control/stage and original cause. Do not catch arbitrary JVM `Error` types. Document rejection uses a message explaining the prohibited DTD/DOCTYPE or blocked external-resolution policy and retains the SAX cause. Other XML/I/O failures retain truthful parsing diagnostics without being mislabeled as attacks. The existing file convenience method can retain its outer read/parse wrapper provided the inner diagnostic and cause remain inspectable.

Keep public parser signatures and `ThemeParseException` as the result contract. Introduce only a package-private factory-supplier constructor/helper seam in `XmlThemeParser` for deterministic unsupported-control tests; preserve the public no-argument construction used by `ParserFactory`. This avoids global provider-property changes or a new public abstraction. No new logging/activator dependency is needed in the security path.

Alternative: a structured diagnostics redesign could improve UI reporting but would broaden API/repository/UI scope. Existing caller exception handling is sufficient for this slice.

### 4. Verify behavior with focused tests using the existing harness

Place parser tests in the runtime bundle's existing `test/com/github/eclipsethemes/core/parser/` package. First reproduce the audit's harmless local-entity injection against the production parser with temporary fixtures. Capture the baseline metadata/token interpretation of safe representative themes before changing the parser. No exploit or fixture accesses real user data.

After hardening, exercise local external general entities, network DTDs, external parameter entities, empty/internal-only DTDs, and a small nested internal-entity fixture. Assert `ThemeParseException`, retained causes, and no theme. Use a monitored resolver around the secured builder and a bounded loopback listener to verify zero resolution callbacks and zero network connections for rejected DTD input; rejection alone is not sufficient evidence of zero external access. Combine local-file fixtures with control/guard assertions so tests establish rejection before external resolution rather than merely absence of an injected token.

Use the helper directly for XInclude and schema-location no-retrieval tests to avoid the existing activator-dependent unknown-element warning path. Also test the guard independently with a builder/resolver spy: an attempted callback must throw before any external stream can be supplied. Exercise production file/stream/source-metadata entry points with prohibited fixtures; caller inspection already establishes bundled/imported/installed delegation, so no repository rewrite is required.

Inject factories/builders that reject each required setting, throw during readback, return unsafe values, or fail creation. A counting input stream must show zero reads and spies must show no insecure builder use/retry. Assert control/stage diagnostics without matching provider-specific full error strings. Verify current safe metadata/token/style/alias/fallback behavior, ordinary character references, repeated parses, and successful parsing of all 33 bundled themes; use a test-only Eclipse logging stand-in only if a corpus warning path requires it, without changing production logging.

Run focused tests on Java 17, then the full reactor on the documented newer build JVM (CI uses JDK 21). Record the JAXP provider/JDK used. An Eclipse runtime smoke check should load the bundled catalog and confirm failure for a harmless DTD fixture to cover provider discovery inside OSGi. Packaging/p2 installation and an OS rendering matrix are not required unless implementation changes those boundaries.

## Risks / Trade-offs

- [Provider rejects a required control or readback] → fail closed with a named stage/control; verify the Java 17 and Eclipse providers. Availability loss is preferable to insecure fallback, and provider compatibility must not be claimed from source inspection alone.
- [DTD-dependent installed themes stop loading] → document that DOCTYPE/custom declarations are unsupported; offer guidance to flatten trusted theme data offline rather than enable external resolution.
- [Import failure can overwrite/delete a prior stored file under existing Q4 behavior] → disclose this current limitation; use isolated temporary storage for any runtime fixture and defer replacement safety to its own R2 change. Parser hardening supplies no compensating file rollback.
- [Security settings accidentally alter ordinary XML interpretation] → retain DOM/namespace policy and assert corpus, character-reference, and representative-output compatibility.
- [Secure processing is mistaken for comprehensive denial-of-service protection] → reject DTD/entity expansion, enable secure processing, and explicitly defer arbitrary input-size/depth quotas.
- [Test probes change process-wide XML/network state] → use local factory injection and monitored loopback resources; dispose listeners and temporary files deterministically. Avoid global provider or URL-handler changes.

## Migration Plan

Implementation is a runtime parser fix using JavaSE-17 APIs in the existing bundle. No manifest exports, extensions, target definitions, persistence migration, preference ownership changes, SWT resources, or new thread scheduling are planned. Callers retain their current stream ownership; the file convenience method continues to close its opened stream. DOM builders remain local to each parse, including when the shared parser is reused.

Before shipping, complete the focused/provider/runtime checks, update the security compatibility note, and review the full diff. On runtime configuration failure, no theme is returned and no parser-driven state commit occurs; existing repository failure ordering remains unchanged. No recovery migration is required. Any release rollback that removes hardening would reintroduce Q2 and must not be presented as a security-safe recovery path; correct a provider compatibility problem while retaining fail-closed behavior.
