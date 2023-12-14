# Changelog


All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## Unreleased
---


## 1.2.0 - 2023-12-14


### Added

- none

### Changed

- adjusted properties for installation-guide
- replace OwnerDTO with UserDTO
- Tests SpaceOwnerController
- changed setOwners Endpoint for Organizations and Spaces <br>
  --> introduced Queryparam `type` to set owners either by email oder userId
- "loadingzone" is no longer a space
- updated logging-library

### Removed

- OwnerDTO

## 1.1.3 - 2023-11-23


### Added

- none

### Changed

- fix rights bugs updating space / organizations

### Removed

- none

## 1.1.2 - 2023-11-22


### Added

- none

### Changed

- fix rights bug assigning roles
- fix bug getting all organizations

### Removed

- none

## 1.1.1 - 2023-11-22


### Added

- none

### Changed

- explicitly reference DB sequences in models
- endpoint to get all spaces (by permissions)
- update log level in global exception handler

### Removed

- none

---


## 1.1.0 - 2023-10-05


### Added

- implement missing audit logs
- fix dependency track findings
- contributing guide

### Changed

- none

### Removed

- none

## 1.0.0 - 2023-09-28


### Added

- initial release

### Changed

- none

### Removed

- none
