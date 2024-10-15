#pragma once

#include <vector>
#include <fstream>

struct Color {
    uint8_t r, g, b, a;
};

enum class BitmapFmt {
    BGRA,
};

class Bitmap final {
    std::vector<uint8_t> pixels;

public:
    int width;
    int height;
    BitmapFmt format;
    uint8_t *data;

    explicit Bitmap(int width, int height,
                    BitmapFmt format = BitmapFmt::BGRA)
            : width(width), height(height), format(format) {
        pixels.resize(width * height * 4);
        data = pixels.data();
    }

    void FillPixels(void *srcData, int srcRowPitch) {
        uint8_t *dstData = pixels.data();
        int dstRowPitch = width * 4;
        for (int y = 0; y < height; y++) {
            std::copy_n(static_cast<uint8_t *>(srcData) + y * srcRowPitch,
                        dstRowPitch,
                        dstData + y * dstRowPitch);
        }
    }

    Color GetColor(int x, int y) {
        uint8_t *colorData = data + (y * width * 4) + (x * 4);
        const uint8_t b = colorData[0];
        const uint8_t g = colorData[1];
        const uint8_t r = colorData[2];
        const uint8_t a = colorData[3];
        return Color{r, g, b, a};
    }

    void SetColor(int x, int y, Color color) {
        uint8_t *colorData = data + (y * width * 4) + (x * 4);
        colorData[0] = color.b;
        colorData[1] = color.g;
        colorData[2] = color.r;
        colorData[3] = color.a;
    }

    void WriteToFile(std::string &&filePath) {
        std::ofstream file(filePath, std::ios::binary);
        file.write(reinterpret_cast<char *>(&pixels[0]),
                   width * height * 4);
    }
};