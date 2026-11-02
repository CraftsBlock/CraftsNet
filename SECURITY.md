# Security Policy

## Supported Versions

CraftsNet provides security support for the latest stable release of the project.

Security vulnerabilities should always be reported against the latest stable release whenever possible. Reports affecting older versions will be evaluated on a case-by-case basis. We strongly recommend keeping your CraftsNet installation up to date to ensure that you benefit from the latest security fixes and improvements.

Security vulnerabilities affecting experimental, development, or unreleased builds may be addressed, but do not necessarily warrant a public security advisory.

If a vulnerability also affects a newer stable release, it will be considered regardless of whether it was originally discovered in an older version.

## Reporting a Vulnerability

If you discover a potential security vulnerability in CraftsNet, please report it through GitHub's private vulnerability reporting mechanism:

https://github.com/CraftsBlock/CraftsNet/security/advisories/new

If private vulnerability reporting is unavailable, please contact the CraftsBlock team through our official contact page:

https://dc.craftsblock.de/

Please do not report security vulnerabilities through public GitHub Issues, discussions, Discord channels, or other public communication channels. Public disclosure before a vulnerability has been investigated and addressed could put CraftsNet users at risk.

When submitting a report, please include as much of the following information as possible:

- A clear description of the vulnerability.
- The affected CraftsNet version(s).
- Steps to reproduce the vulnerability.
- The potential security impact.
- A proof of concept (PoC), if available.
- Any relevant logs, screenshots, or additional technical details.

We appreciate responsible disclosure and ask security researchers to give us a reasonable amount of time to investigate and address reported vulnerabilities before making them public.

## Response and Remediation Process

We make every reasonable effort to acknowledge incoming security reports as soon as possible.

After receiving a report, the CraftsBlock team will:

1. Review the report and determine whether the issue represents a valid security vulnerability.
2. Assess its severity, potential impact, and affected versions.
3. Investigate and reproduce the reported vulnerability where possible.
4. Develop and review an appropriate fix or mitigation.
5. Release a patched version of CraftsNet.
6. Publish a GitHub Security Advisory containing relevant information about the vulnerability and its remediation.

Depending on the complexity and severity of the vulnerability, this process may take some time. We may contact the reporter to request additional information or assistance with verification.

## Disclosure Policy

We follow a coordinated vulnerability disclosure process.

Security vulnerabilities will generally remain private while they are being investigated and remediated.

Once a fix has been released, we aim to publicly disclose the vulnerability through a GitHub Security Advisory approximately 30 days after the patched release.

This period gives users and developers reasonable time to update their dependencies before technical details become publicly available.

The disclosure timeline may be adjusted depending on the severity of the vulnerability, the availability of a fix, and other relevant circumstances.

We kindly ask security researchers to refrain from publicly disclosing vulnerability details until the coordinated disclosure process has been completed.

## Scope

This security policy covers security vulnerabilities directly affecting:

- CraftsNet and its core functionality.
- HTTP route handling and request processing.
- WebSocket connections and message handling.
- Addon loading, registration, and execution.
- Authentication, authorization, and access control mechanisms implemented by CraftsNet.
- SSL/TLS handling and related security functionality.
- Security-sensitive integrations and functionality provided by CraftsNet.

Vulnerabilities in third-party dependencies should generally be reported to the respective maintainers. However, if a vulnerability in a dependency directly affects CraftsNet, we encourage you to report its impact on CraftsNet as well.

## Recognition

We appreciate the work of security researchers and contributors who help us improve the security of CraftsNet.

With the reporter's consent, contributors may be publicly acknowledged in the Credits section of the corresponding GitHub Security Advisory.

We do not currently offer a monetary bug bounty program.

Thank you for helping us keep CraftsNet and its users safe.
