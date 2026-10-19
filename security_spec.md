# Security Specification — Smart Digital Wardrobe

## 1. Data Invariants
1. **Authentication & Ownership**: Every document in `/wardrobe/{itemId}` must belong to an authenticated user (`request.auth != null`) and have `userId == request.auth.uid`.
2. **Strict Schema & Enums**:
   - `name`: string, 1..100 characters
   - `imageUrl`: string, 1..900,000 characters
   - `category`: strictly one of `['Top', 'Outerwear', 'Bottom', 'Shoes']`
   - `weather`: strictly one of `['Cold', 'Mild', 'Hot', 'All-season']`
   - `style`: strictly one of `['Casual', 'Formal/Smart', 'Sport']`
   - `createdAt`, `updatedAt`: server timestamps (`is timestamp && <= request.time`)
3. **Immutability**: `userId` and `createdAt` cannot be modified after creation.
4. **Query Isolation**: `list` queries on `/wardrobe` must filter by `resource.data.userId == request.auth.uid`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read**: `get` or `list` on `/wardrobe` with `auth == null`.
2. **Cross-User Read**: Alice attempts `get` on `/wardrobe/bob_item` where `userId == "bob_456"`.
3. **Unfiltered List Query**: Alice queries `/wardrobe` without `.where("userId", "==", "alice_123")`.
4. **Identity Spoofing on Create**: Alice creates an item with `userId: "bob_456"`.
5. **Shadow / Ghost Field Injection**: Alice creates or updates an item including `"isAdmin": true`.
6. **Invalid Category Enum**: Alice writes `category: "Accessory"` (not in allowlist).
7. **Invalid Weather Enum**: Alice writes `weather: "Freezing"` (not in allowlist).
8. **Invalid Style Enum**: Alice writes `style: "Grunge"` (not in allowlist).
9. **Oversized Name DoS**: Alice writes a `name` with 500 characters (`> 100`).
10. **Future Timestamp Spoofing**: Alice writes `createdAt` set in the future (`> request.time`).
11. **Immutable Field Mutation**: Alice updates `userId` or `createdAt` on an existing document.
12. **Cross-User Delete**: Bob attempts to delete Alice's document `/wardrobe/alice_item`.

## 3. Red Team Audit Report
- **Shadow Update Test**: Passed — `incoming().keys().hasOnly(...)` on create and `affectedKeys().hasOnly(...)` on update block any extra fields.
- **Query Trust Test**: Passed — `allow list` explicitly checks `existing().userId == request.auth.uid`.
- **Value Poisoning Test**: Passed — `isValidWardrobeItem(incoming())` wraps both `create` and `update`.
