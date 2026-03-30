package zzk.townshipscheduler.ui.views.crawling;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;

public class UploadComponentPlayground
        extends VerticalLayout {

    public UploadComponentPlayground() {
        //demo1
        InMemoryUploadHandler inMemoryUploadHandler
                = UploadHandler.inMemory((metadata, data) -> {
            long contentLength = metadata.contentLength();
            String fileName = metadata.fileName();
            String contentedType = metadata.contentType();

            processBytes(data);
        });
        Upload upload = new Upload(inMemoryUploadHandler);
        //demo2
        upload.setDropAllowed(true);
        //demo3
        upload.setAutoUpload(false);
        //demo4
        upload.setAcceptedFileTypes("application/pdf", ".pdf");
        upload.addFileRejectedListener(rejectedEvent -> {
            String errorMessage = rejectedEvent.getErrorMessage();
            Notification show = Notification.show(errorMessage);
            show.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });
        //demo5
        upload.setMaxFiles(1);
        //demo6
        //spring.servlet.multipart.max-file-size=50MB
        //spring.servlet.multipart.max-request-size=100MB
        //spring.servlet.multipart.enabled=false
        int fileSizeLimit = 10 * 1024 * 1024;//10mb
        upload.setMaxFileSize(fileSizeLimit);


        //UploadHandler.inMemory()
        //UploadHandler.toFile()
        //UploadHandler.toTempFile()
        UploadHandler mySimpleUploadHandler = (event) -> {
            try (InputStream inputStream = event.getInputStream();) {
                doSomethingInputSteam(inputStream);

            }
            catch (Exception e) {

            }
        };

        //demo8
        UploadHandler.toTempFile(
                        (metadata, file) -> System.out.printf(
                                "File saved to: %s%n",
                                file.getAbsolutePath()
                        )
                )
                .whenStart(() -> System.out.println("Upload started"))
                .onProgress((transferredBytes, totalBytes) -> {
                    double percentage = (double) transferredBytes / totalBytes * 100;
                    System.out.printf("Upload progress: %.2f%%\n", percentage);
                })
                .whenComplete((success) -> {
                    if (success) {
                        System.out.println("Upload completed successfully");
                    } else {
                        System.out.println("Upload failed");
                    }
                })
        ;

//        TransferProgressListener progressListener = new TransferProgressListener() {
//            @Override
//            public void onStart(TransferContext context) {
//                Assert.assertEquals(165000, context.contentLength());
//                Assert.assertEquals("download", context.fileName());
//                invocations.add("onStart");
//            }
//
//            @Override
//            public void onProgress(
//                    TransferContext context,
//                    long transferredBytes, long totalBytes) {
//                double percentage = (double) transferredBytes / totalBytes * 100;
//                System.out.printf("Upload progress: %.2f%%\n", percentage);
//            }
//
//            @Override
//            public void onComplete(TransferContext context,
//                    long transferredBytes) {
//                System.out.println("Upload completed successfully");
//            }
//
//            @Override
//            public void onError(TransferContext context,
//                    IOException reason) {
//                System.out.println("Upload failed");
//            }
//        };

        CustomUploadHandler uploadHandler = new CustomUploadHandler()
                .whenStart(() -> System.out.println("Upload started"))
                .onProgress((transferredBytes, totalBytes) -> {
                    double percentage = (double) transferredBytes / totalBytes * 100;
                    System.out.printf("Upload progress: %.2f%%\n", percentage);
                })
                .whenComplete((success) -> {
                    if (success) {
                        System.out.println("Upload completed successfully");
                    } else {
                        System.out.println("Upload failed");
                    }
                });

//        UploadHandler.toTempFile(
//                (metadata, file) -> System.out.printf("File saved to: %s%n",
//                        file.getAbsolutePath()), progressListener);
    }

    private void doSomethingInputSteam(InputStream inputStream) {

    }

    private void processBytes(byte[] data) {

    }

     class CustomUploadHandler
            extends TransferProgressAwareHandler<UploadEvent, CustomUploadHandler>
            implements UploadHandler {
        @Override
        public void handleUploadRequest(UploadEvent event) {
            try (InputStream inputStream = event.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream();) {
                // Use the TransferUtil.transfer method to copy the data
                // to notify progress listeners
                TransferUtil.transfer(
                        inputStream,
                        outputStream,
                        getTransferContext(event),
                        getListeners());
                // Process the data
                byte[] data = outputStream.toByteArray();
                // ...
            } catch (IOException e) {
                // Notify listeners of the error
                notifyError(event, e);
                throw new UncheckedIOException(e);
            }
        }
        @Override
        protected TransferContext getTransferContext(UploadEvent event) {
            return new TransferContext(
                    event.getRequest(),
                    event.getResponse(),
                    event.getSession(),
                    event.getFileName(),
                    event.getOwningElement(),
                    event.getFileSize());
        }
    }

    class MySelfDefineUploadHandler
            implements UploadHandler {

        @Override
        public void handleUploadRequest(UploadEvent event) throws IOException {

        }

        @Override
        public void responseHandled(boolean success, VaadinResponse response) {
            UploadHandler.super.responseHandled(success, response);
        }

        @Override
        public void handleRequest(VaadinRequest request, VaadinResponse response, VaadinSession session, Element owner) throws IOException {
            UploadHandler.super.handleRequest(request, response, session, owner);
        }

        @Override
        public long getRequestSizeMax() {
            return UploadHandler.super.getRequestSizeMax();
        }

        @Override
        public long getFileSizeMax() {
            return UploadHandler.super.getFileSizeMax();
        }

        @Override
        public long getFileCountMax() {
            return UploadHandler.super.getFileCountMax();
        }

    }

    /**
     * Provides a default I18N configuration for the Upload examples
     * <p>
     * At the moment the Upload component requires a fully configured I18N instance,
     * even for use-cases where you only want to change individual texts.
     * <p>
     * This I18N configuration is an adaption of the web components I18N defaults
     * and can be used as a basis for customizing individual texts.
     */
    class UploadExamplesI18N
            extends UploadI18N {

        public UploadExamplesI18N() {
            setDropFiles(new DropFiles().setOne("Drop file here")
                    .setMany("Drop files here"));
            setAddFiles(new AddFiles().setOne("Upload File...")
                    .setMany("Upload Files..."));
            setError(new Error().setTooManyFiles("Too Many Files.")
                    .setFileIsTooBig("File is Too Big.")
                    .setIncorrectFileType("Incorrect File Type."));
            setUploading(new Uploading()
                    .setStatus(new Uploading.Status().setConnecting("Connecting...")
                            .setStalled("Stalled")
                            .setProcessing("Processing File...")
                            .setHeld("Queued"))
                    .setRemainingTime(new Uploading.RemainingTime()
                            .setPrefix("remaining time: ")
                            .setUnknown("unknown remaining time"))
                    .setError(new Uploading.Error()
                            .setServerUnavailable(
                                    "Upload failed, please try again later")
                            .setUnexpectedServerError(
                                    "Upload failed due to server error")
                            .setForbidden("Upload forbidden")));
            setUnits(new Units().setSize(Arrays.asList(
                    "B", "kB", "MB", "GB", "TB",
                    "PB", "EB", "ZB", "YB"
            )));
        }

    }

}
