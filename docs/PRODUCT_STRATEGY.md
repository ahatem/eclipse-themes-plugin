# Product Strategy — Eclipse Themes

## Product promise

Become a trusted, open place to manage **supported** Eclipse appearance surfaces safely and coherently. Do not promise control over every pixel, every third-party widget, native OS chrome, or arbitrary icons.

## Strategic blockers from the audit

1. **Trust:** a customization plugin cannot become canonical while it can erase unrelated user settings. Preservation, recovery, and truthful outcomes are prerequisites.
2. **Competition:** built-in Eclipse themes and products such as DevStyle already cover parts of the desired scope. More palettes alone are not a durable differentiator.
3. **Maintenance:** broad editor/OS/Eclipse-version support creates continuous integration work. A bounded published support matrix is required.
4. **Discovery/adoption:** Marketplace presence is insufficient; screenshots, compatibility clarity, reliable installs, release notes, and contributor ergonomics matter.
5. **Upstream/community:** broad adoption depends on useful public contributor APIs, collaboration with Eclipse projects, and upstreaming missing platform capabilities where practical.
6. **Licensing/provenance:** icons, fonts, CSS, and imported theme assets need documented rights and notices.

## Differentiators worth investing in

Priority order from the audit:

- safe preference ownership and one-click recovery;
- offline previews generated from actual local theme data;
- portable coordinated theme packages with provenance;
- an honest compatibility/coverage inspector;
- accessibility diagnostics and high-contrast preservation;
- independent mixing of editor syntax, workbench, fonts, and participating icons;
- modern TM4E/LSP-oriented editor support;
- theme authoring/validation tooling tied to real Eclipse output.

## Conditions for success

### Technical

- reversible changes and conflict-aware restoration;
- stable identities/revisions;
- faithful previews;
- bounded compatibility claims with tests;
- unsupported surfaces explicitly identified.

### Social

- contributors can add an integration without understanding the whole application;
- predictable releases;
- more than one maintainer/integration owner over time;
- healthy upstream relationships.

### Legal

- asset provenance and notices are documented;
- imported themes remain declarative rather than executable;
- project branding does not imply unsupported Eclipse Foundation endorsement.

### Operational

The project must be able to maintain its promised compatibility matrix across quarterly Eclipse releases.

## Strategic interpretation

"Canonical" is an adoption outcome, not a feature flag. The engineering target is to become the most trustworthy and interoperable open solution first; community adoption can follow from that.
