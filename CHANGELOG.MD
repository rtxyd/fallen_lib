# Changelog

## 1.3
---
### 1.3.4
## Runtime.forgemod
- Crude SyncableRegistry, with network handler (now it's poor version of `DynamicRegistry` in Placebo)
- Simple 1.21.1 like Connection for network
- 1.3.3.1/1.3.3.2 -> Implement `AbstractPacketBoundRegistry`, `AbstractRegistryBoundPacketPayload`, `AbstractSingleEntryPacketPayLoad`

### 1.3.3
## Core
- Mod locator tweak
## Runtime.forgemod
- Special check for FGA version compat
- Attrlib fix port
- Lootcategory `forItem()` fix

### 1.3.2
- Implement a simple delegating mixin connector registry
- Add empty checks for fallen config json file

### 1.3.1
- Distribute runtime environment
- Add experimental build task (for future use)
- Fix `BuildInfo`

### 1.3.0
- Experimental simple class merge engine (very low quality)
- Implement fallen lib forge runtime mod
- Port extra_gem_bonus from Fallen Gems & Affixes
- Implement fallen lib forge mod lifecycle hook

## 1.2
---
### 1.2.0
- Hotfix, fix buildInfo with wrong version.
- Introduce an each-class cache to cache the `MethodInsnNode` state, avoiding recalculation of local variables.
- Update `ObjectModifierFactory`, make the ignored field filter a `Predicate<String>` to support more flexible control.
- Add `inserter` field to `@FallenPatch` define the class contains methods with `@FallenInserter`
- Introduce `FallenInserter` annotation and parsing.
- Package structure change, `fallen_lib` -> `fallen.lib`
- Introduce standard method inserter.
- Introduce patch util.

## 1.1
---
### 1.1.2
- Fixed entry targets don't include nest members.
- Improved logger behaviors, prevent spamming in log.
- API change, `IFallenPatchContext#getAppliedPatches` -> `IFallenPatchContext#currentClassAppliedPatches`
- Fixed a null point exception when resource is not found.
### 1.1.1
- Added debug infos in patch processing procedure.
- Changed `copyOf()` to unmodifiable version.
- Added context for ctor and patch method `apply()`
- Fixed entries sorting.
- Added an empty check before traversing classes.
- Filtered `minecraft` and `forge` class targets for now.
### 1.1.0
- Reimplemented fallen patch as a configurable API.

## 1.0
---
### 1.0.2
- Aligned mod id and mod main class.
### 1.0.1
- Fixed a `String index out of range` problem when applying transformer.
### 1.0.0
- Initialized.