package com.example.photosapp.util;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Size;

public final class ImageLoader {

    private ImageLoader() {
    }

    public static Bitmap loadThumbnail(Context context, String uriString, int sizePx) {
        try {
            return context.getContentResolver().loadThumbnail(
                    Uri.parse(uriString),
                    new Size(sizePx, sizePx),
                    null
            );
        } catch (Exception ignored) {
            return loadFullImage(context, uriString, sizePx, sizePx);
        }
    }

    public static Bitmap loadFullImage(Context context, String uriString, int maxWidth, int maxHeight) {
        try {
            Uri uri = Uri.parse(uriString);
            ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                int sourceWidth = info.getSize().getWidth();
                int sourceHeight = info.getSize().getHeight();
                if (sourceWidth <= 0 || sourceHeight <= 0 || maxWidth <= 0 || maxHeight <= 0) {
                    return;
                }
                float widthScale = (float) maxWidth / sourceWidth;
                float heightScale = (float) maxHeight / sourceHeight;
                float scale = Math.min(widthScale, heightScale);
                if (scale > 0f && scale < 1f) {
                    decoder.setTargetSize(
                            Math.max(1, Math.round(sourceWidth * scale)),
                            Math.max(1, Math.round(sourceHeight * scale))
                    );
                }
            });
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String resolveDisplayName(Context context, Uri uri) {
        String[] projection = new String[]{OpenableColumns.DISPLAY_NAME};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex);
                }
            }
        } catch (Exception ignored) {
            // Fall through to last path segment.
        }
        String lastPathSegment = uri.getLastPathSegment();
        return lastPathSegment == null ? uri.toString() : lastPathSegment;
    }
}

