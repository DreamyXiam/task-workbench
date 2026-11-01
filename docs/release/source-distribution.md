# Source distribution and network deployments

The original project uses AGPL-3.0-only. Dependencies keep their own terms; see [third-party notices](../../THIRD_PARTY_NOTICES.md) and the [license review](licenses/REVIEW.md).

The application displays a **对应源码 · AGPL-3.0** link before and after login. It defaults to this project's GitHub repository. Before serving a modified version over a network, set `VITE_SOURCE_URL` at frontend build time to an accessible source location for the version actually deployed, including your modifications and the material required to build it. A link to an unchanged upstream repository does not supply your changes. Test the link as an unauthenticated user. The frontend environment variable is public configuration and must contain no credential or signed download token.

```powershell
$env:VITE_SOURCE_URL='https://github.com/YOUR-ACCOUNT/YOUR-REPOSITORY/tree/YOUR-DEPLOYED-COMMIT'
npm run build
```

Publish source with the license, manifests, lockfile, migrations and build instructions. Do not publish credentials, deployment databases, logs or real attachments. Keep an immutable commit/tag or source archive corresponding to each deployed build. A private repository link is not an accessible source offer to users who lack access.

This first open-source preview distributes project source and dependency manifests, not JARs, compiled browser assets, dependency tarballs, node_modules or bundled fonts. The license inventory documents additional corresponding-source and notice work needed before distributing such binaries. Do not attach them as release artifacts until those distribution conditions have been checked.
