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
            filename = "hi_asr.onnx",
            relativePath = "models/asr/hi_asr.onnx",
            downloadUrl = "${ASR_BASE_URL}indic_conformer_hi.onnx",
            sizeBytes = 197_595_593L
        ),
        ModelFileInfo(
            filename = "hi_tokens.txt",
            relativePath = "models/asr/hi_tokens.txt",
            downloadUrl = "${ASR_BASE_URL}tokens.txt",
            sizeBytes = 67_605L
        ),
        ModelFileInfo(
            filename = "sat_asr.onnx",
            relativePath = "models/asr/sat_asr.onnx",
            downloadUrl = "${ASR_BASE_URL}indic_conformer_sat.onnx",
            sizeBytes = 197_584_818L
        ),
        ModelFileInfo(
            filename = "sat_tokens.txt",
            relativePath = "models/asr/sat_tokens.txt",
            downloadUrl = "${ASR_BASE_URL}tokens.txt",
            sizeBytes = 67_605L
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
