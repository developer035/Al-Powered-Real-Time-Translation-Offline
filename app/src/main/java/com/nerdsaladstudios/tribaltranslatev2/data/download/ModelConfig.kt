package com.nerdsaladstudios.tribaltranslatev2.data.download

data class ModelFileInfo(
    val filename: String,
    val relativePath: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

object ModelConfig {
    const val ASR_BASE_URL = "https://huggingface.co/remiai3/STT_MODELS_FOR_APP/resolve/main/"
    const val NMT_BASE_URL = "https://huggingface.co/AI4Bharat/IndicTrans2-ONNX/resolve/main/"

    val REQUIRED_MODELS = listOf(
        ModelFileInfo(
            filename = "hi_asr_int8.onnx",
            relativePath = "models/asr/hi_asr_int8.onnx",
            downloadUrl = "${ASR_BASE_URL}indic_conformer_hi_int8.onnx",
            sizeBytes = 95_000_000L
        ),
        ModelFileInfo(
            filename = "hi_tokens.txt",
            relativePath = "models/asr/hi_tokens.txt",
            downloadUrl = "${ASR_BASE_URL}hi_tokens.txt",
            sizeBytes = 50_000L
        ),
        ModelFileInfo(
            filename = "sat_asr_int8.onnx",
            relativePath = "models/asr/sat_asr_int8.onnx",
            downloadUrl = "${ASR_BASE_URL}indic_conformer_sat_int8.onnx",
            sizeBytes = 95_000_000L
        ),
        ModelFileInfo(
            filename = "sat_tokens.txt",
            relativePath = "models/asr/sat_tokens.txt",
            downloadUrl = "${ASR_BASE_URL}sat_tokens.txt",
            sizeBytes = 50_000L
        ),
        ModelFileInfo(
            filename = "nmt_hi2sat_encoder_int8.onnx",
            relativePath = "models/nmt/nmt_hi2sat_encoder_int8.onnx",
            downloadUrl = "${NMT_BASE_URL}hin_Deva-sat_Olck/encoder_model_quantized.onnx",
            sizeBytes = 110_000_000L
        ),
        ModelFileInfo(
            filename = "nmt_hi2sat_decoder_int8.onnx",
            relativePath = "models/nmt/nmt_hi2sat_decoder_int8.onnx",
            downloadUrl = "${NMT_BASE_URL}hin_Deva-sat_Olck/decoder_model_quantized.onnx",
            sizeBytes = 110_000_000L
        ),
        ModelFileInfo(
            filename = "nmt_sat2hi_encoder_int8.onnx",
            relativePath = "models/nmt/nmt_sat2hi_encoder_int8.onnx",
            downloadUrl = "${NMT_BASE_URL}sat_Olck-hin_Deva/encoder_model_quantized.onnx",
            sizeBytes = 110_000_000L
        ),
        ModelFileInfo(
            filename = "nmt_sat2hi_decoder_int8.onnx",
            relativePath = "models/nmt/nmt_sat2hi_decoder_int8.onnx",
            downloadUrl = "${NMT_BASE_URL}sat_Olck-hin_Deva/decoder_model_quantized.onnx",
            sizeBytes = 110_000_000L
        ),
        ModelFileInfo(
            filename = "indictrans2_spm.model",
            relativePath = "models/nmt/indictrans2_spm.model",
            downloadUrl = "${NMT_BASE_URL}indictrans2_spm.model",
            sizeBytes = 4_500_000L
        )
    )

    fun getTotalSizeBytes(): Long = REQUIRED_MODELS.sumOf { it.sizeBytes }
}
