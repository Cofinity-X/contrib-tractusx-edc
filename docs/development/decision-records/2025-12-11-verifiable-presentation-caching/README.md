# Verifiable Presentation Caching

## Decision

We will implement a caching mechanism for Verifiable Presentations (VPs) within the DCP communication flow. Each
requested VP will be cached and before any new presentation request is made, the cache is checked for a matching VP
first.

## Rationale

For each DSP message exchanged, the receiving connector requests a VP of the sending participant. This includes multiple
requests to the wallet (sending participant's STS, receiving participant's STS, sending participant's presentation API).
This causes quite high network traffic, as e.g. during a contract negotiation at least 4 DSP messages are exchanged,
i.e. the whole request sequence will be run at least 4 times during a single contract negotiation.

As available Verifiable Credentials (VCs) do not frequently change, part of the request sequence can be omitted after
the whole sequence has been executed once by introducing a cache for VPs. The initial call to the sending participant's
STS always needs to be made, as the receiving participant may not have any VPs cached. But after initially requesting a
VP for a participant, the requests to the receiving participant's STS as well as to the sending participant's
presentation API can be skipped for subsequent DSP messages exchanged with the same participant, thus greatly reducing
network traffic.

## Approach

### VerifiablePresentationCache

First, we need to define an interface for the cache. It will provide methods for storing a new entry, retrieving an
entry and removing entries for a participant. As each VP is requested for a specific participant and specific scopes,
both `counterPartyDid` and `scopes` need to be used for storing and retrieving entries. For deleting, there will be two 
options: deleting all entries for a participant and deleting a single entry for a participant with given scopes.
As starting from EDC version `0.15.0` the `participantContextId` is passed to the `DcpIdentityService` and
`PresentationRequestService`, this should also be included in the cache.

```java
import java.util.List;

public interface VerifiablePresentationCache {
    
    StoreResult<Void> store(String participantContextId, String counterPartyDid, List<String> scopes, List<VerifiablePresentationContainer> presentations);
    
    StoreResult<List<VerifiablePresentationContainer>> query(String participantContextId, String counterPartyDid, List<String> scopes);

    StoreResult<Void> remove(String participantContextId, String counterPartyDid, List<String> scopes);
    
    StoreResult<Void> remove(String participantContextId, String counterPartyDid);
}
```

The interface will be located in a new, dedicated spi module `dcp-spi`. Additionally, a model class
`VerifiablePresentationCacheEntry` will be added to this spi module, which encapsulates all values to be cached as well
as the timestamp at which the cache entry was created.

#### AbstractVerifiablePresentationCache

To decouple common cache behavior from the underlying persistence layer, we will create an abstract implementation
of `VerifiablePresentationCache`, which takes care of checking the validity of an entry, but leaves the actual
storing of entries to the implementing classes.

To ensure that no invalid VPs are returned from the cache, i.e. no expired or revoked VCs or VCs with invalid issuers,
as these would cause a validation failure later on, the `AbstractVerifiablePresentationCache` will utilize the
`VerifiableCredentialValidationService`. This will lead to duplication of some checks, as they will be run once within
the cache implementation and once later on in the `DcpIdentityService`, but as all checks executed in the
`VerifiableCredentialValidationService` are lightweight, this should not be an issue.

```java
public abstract class AbstractVerifiablePresentationCache implements VerifiablePresentationCache {
    
    // ...

    public StoreResult<Void> store(String participantContextId, String counterPartyDid, List<String> scopes,
                                   List<VerifiablePresentationContainer> presentations) {
        var entry = new VerifiablePresentationCacheEntry(participantContextId, counterPartyDid, scopes, presentations, Instant.now(clock));
        return storeInternal(entry);
    }

    public StoreResult<List<VerifiablePresentationContainer>> query(String participantContextId, String counterPartyDid, List<String> scopes) {
        var cacheResult = queryInternal(participantContextId, counterPartyDid, scopes);

        if (cacheResult.failed()) {
            return StoreResult.notFound("No cached entry found for given participant and scopes.");
        }

        if (isExpired(cacheResult.getContent()) || !areCredentialsValid(cacheResult.getContent().getPresentations(), participantContextId)) {
            remove(participantContextId, counterPartyDid, scopes);
            return StoreResult.notFound("No cached entry found for given participant and scopes.");
        }

        return cacheResult.map(VerifiablePresentationCacheEntry::getPresentations);
    }

    protected abstract StoreResult<Void> store(VerifiablePresentationCacheEntry entry);

    protected abstract StoreResult<VerifiablePresentationCacheEntry> queryEntry(String participantContextId, String counterPartyDid, List<String> scopes);

    private boolean isExpired(VerifiablePresentationCacheEntry entry) {
        // ...
    }

    private boolean areCredentialsValid(List<VerifiablePresentationContainer> presentations, String participantContextId) {
        // ...
    }
}
```

#### Cache Implementations

As runtimes are provided for both in-memory and persistent deployments, there will be two implementations of
`VerifiablePresentationCache`: the `InMemoryVerifiablePresentationCache` and the `SqlVerifiablePresentationCache`.
The former will be the default implementation provided as part of the same module as the
[CachePresentationRequestService](#cachepresentationrequestservice), while the latter will be made available in a
separate module. Both implementations will extend the `AbstractVerifiablePresentationCache`.

Usually, a cache should be purely in-memory, i.e. not actually persisted, but as EDCs may be downscaled when no
processes are running, there will also be an SQL implementation to not lose the benefits of caching VPs in scenarios
where EDCs are frequently downscaled.

### CachePresentationRequestService

To include the cache in the DCP flow, a custom implementation of `PresentationRequestService` needs to be provided.
This service encapsulates the steps of creating an SI token for the receiving participant and requesting the sending
participant's VP. The `DefaultPresentationRequestService` is available in the `dcp-lib` module and will be extended by
the custom implementation as to not duplicate the existing code. The custom implementation will wrap the existing code
with calls to the cache:

```java
public class CachePresentationRequestService extends DefaultPresentationRequestService {
    
    private final VerifiablePresentationCache cache;
    
    // ...

    @Override
    public Result<List<VerifiablePresentationContainer>> requestPresentation(String participantContextId, String ownDid,
                                                                             String counterPartyDid, String counterPartyToken,
                                                                             List<String> scopes) {
        var cacheResult = cache.query(participantContextId, counterPartyDid, scopes);
        if (cacheResult.succeeded()) {
            return Result.success(cacheResult.getContent());
        }

        var vpResult = super.requestPresentation(participantContextId, ownDid, counterPartyDid, counterPartyToken, scopes);

        if (vpResult.succeeded()) {
            cache.store(participantContextId, counterPartyDid, scopes, vpResult.getContent());
        }

        return vpResult;
    }
}
```

The `CachePresentationRequestService` will be added as part of the new module `verifiable-presentation-cache` located
in the `dcp` super-module. In the same module, the `InMemoryVerifiablePresentationCache` will be provided in a
separate extension using a default provider.

### Cache Invalidation API

Even though the VCs are checked also within the cache, there may be situations where an invalid VP is cached, e.g.
when a VC defined in the requested scopes was initially missing and shortly after added to the wallet. To not block
communication in these situations, there needs to be a way to trigger removal of cache entries. For this purpose,
we'll add an API, which will comprise a single endpoint which takes a participant ID as parameter and removes all cache
entries for that participant when called.

### Configuration

The cache will be enabled by default and have a default validity period of 24 hours. The validity period will be
configurable via a new setting `tx.edc.dcp.cache.validity.seconds`. Additionally, for cases where no cache should be
used, the cache can be disabled by setting the validity to `0`.
