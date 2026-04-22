package net.rtxyd.fallen.lib.util;

import java.util.Optional;
import java.util.function.Consumer;

public interface ISingleUseAsyncWorker extends IAsyncWorker {

    <T> Optional<T> readAndBurn(String id, Consumer<Exception> processEx);

    <T> T readAndBurnOrThrow(String id);
}
