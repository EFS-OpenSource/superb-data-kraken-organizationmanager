### Organization and Space Management


JSON-based, REST-like API for managing organizations and spaces.

SDK is organized in organizations and spaces, where a space represents a use-case and an organization packages use-cases.

On a technical level, an organization corresponds to a Storage Account, whereas a space corresponds to a Container. Each organization has a dedicated "
Container" called 'loadingzone', which
serves as an intermediate store for incoming data. After processing, this data will be moved to the main-storage (target-space) - however this is out of this
service's
scope.

In addition to organizations and spaces, this service manages the authorization to these. in addition to organizations and spaces, the organizationmanager
manages the authorization for these. For information on which roles exist and what they are authorized for, please refer to the roles/rights concept (**
TODO:** <insert-public-link-here>).
If users require authorization to a certain organization/space, they can request access ("`UserRequest`") with the desired roles. An administrator (or owner)
can then grant permission or deny it. An administrator (or owner) can also directly grant permissions to users.

---
**NOTE on confidentiality**

The organizations confidentiality marks the minimum-confidentiality of its spaces. Though a PRIVATE organization may have PUBLIC spaces, they will be ignored in
role-checks.

---