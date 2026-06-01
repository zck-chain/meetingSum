package com.meetingsum.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String appName = "MeetingSum";
    private String appVersion = "0.1.0";
    private boolean debug = true;

    private String asrProvider = "whisper_local";
    private String whisperModel = "medium";
    private String whisperDevice = "cpu";
    private String whisperScriptPath = "whisper_transcribe.py";

    private String llmProvider = "claude";
    private String anthropicApiKey = "";
    private String anthropicModel = "claude-sonnet-4-6";
    private String deepseekApiKey = "";
    private String deepseekModel = "deepseek-chat";
    private String deepseekBaseUrl = "https://api.deepseek.com";

    private int maxFileSizeMb = 2048;
    private int maxVideoDurationSeconds = 14400;
    private String allowedFormats = "mp4,mov,avi,mkv,mp3,wav,m4a,webm";

    private String ffmpegPath = "ffmpeg";
    private String ffprobePath = "ffprobe";

    private String uploadDir = "./uploads";
    private String outputDir = "./outputs";

    // ---- Phase 2: 摘要模板 ----
    private String summaryTemplate = "";

    // ---- Phase 2: pyannote 说话人分离 ----
    private boolean pyannoteEnabled = false;
    private String pyannoteScriptPath = "pyannote_diarize.py";
    private String pyannoteDevice = "cpu";
    private String pyannoteHfToken = "";

    // ---- Phase 2: 超时控制 ----
    private TimeoutConfig timeout = new TimeoutConfig();

    public long getMaxFileSizeBytes() {
        return (long) maxFileSizeMb * 1024 * 1024;
    }

    public List<String> getAllowedFormatList() {
        return Arrays.asList(allowedFormats.split(","));
    }

    // ---- 嵌套类：超时配置 ----

    public static class TimeoutConfig {
        /** 全局管道超时（秒），默认 30 分钟 */
        private int globalSeconds = 1800;
        /** FFmpeg 音频提取超时 */
        private int audioExtractionSeconds = 600;
        /** Whisper 转写超时 */
        private int transcriptionSeconds = 1200;
        /** LLM 摘要超时 */
        private int summarizationSeconds = 300;
        /** 文档导出超时 */
        private int exportSeconds = 60;

        public int getGlobalSeconds() { return globalSeconds; }
        public void setGlobalSeconds(int globalSeconds) { this.globalSeconds = globalSeconds; }

        public int getAudioExtractionSeconds() { return audioExtractionSeconds; }
        public void setAudioExtractionSeconds(int audioExtractionSeconds) { this.audioExtractionSeconds = audioExtractionSeconds; }

        public int getTranscriptionSeconds() { return transcriptionSeconds; }
        public void setTranscriptionSeconds(int transcriptionSeconds) { this.transcriptionSeconds = transcriptionSeconds; }

        public int getSummarizationSeconds() { return summarizationSeconds; }
        public void setSummarizationSeconds(int summarizationSeconds) { this.summarizationSeconds = summarizationSeconds; }

        public int getExportSeconds() { return exportSeconds; }
        public void setExportSeconds(int exportSeconds) { this.exportSeconds = exportSeconds; }
    }

    // ---- Getters and setters ----

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }

    public String getAsrProvider() { return asrProvider; }
    public void setAsrProvider(String asrProvider) { this.asrProvider = asrProvider; }

    public String getWhisperModel() { return whisperModel; }
    public void setWhisperModel(String whisperModel) { this.whisperModel = whisperModel; }

    public String getWhisperDevice() { return whisperDevice; }
    public void setWhisperDevice(String whisperDevice) { this.whisperDevice = whisperDevice; }

    public String getWhisperScriptPath() { return whisperScriptPath; }
    public void setWhisperScriptPath(String whisperScriptPath) { this.whisperScriptPath = whisperScriptPath; }

    public String getLlmProvider() { return llmProvider; }
    public void setLlmProvider(String llmProvider) { this.llmProvider = llmProvider; }

    public String getAnthropicApiKey() { return anthropicApiKey; }
    public void setAnthropicApiKey(String anthropicApiKey) { this.anthropicApiKey = anthropicApiKey; }

    public String getAnthropicModel() { return anthropicModel; }
    public void setAnthropicModel(String anthropicModel) { this.anthropicModel = anthropicModel; }

    public String getDeepseekApiKey() { return deepseekApiKey; }
    public void setDeepseekApiKey(String deepseekApiKey) { this.deepseekApiKey = deepseekApiKey; }

    public String getDeepseekModel() { return deepseekModel; }
    public void setDeepseekModel(String deepseekModel) { this.deepseekModel = deepseekModel; }

    public String getDeepseekBaseUrl() { return deepseekBaseUrl; }
    public void setDeepseekBaseUrl(String deepseekBaseUrl) { this.deepseekBaseUrl = deepseekBaseUrl; }

    public int getMaxFileSizeMb() { return maxFileSizeMb; }
    public void setMaxFileSizeMb(int maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }

    public int getMaxVideoDurationSeconds() { return maxVideoDurationSeconds; }
    public void setMaxVideoDurationSeconds(int maxVideoDurationSeconds) { this.maxVideoDurationSeconds = maxVideoDurationSeconds; }

    public String getAllowedFormats() { return allowedFormats; }
    public void setAllowedFormats(String allowedFormats) { this.allowedFormats = allowedFormats; }

    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

    public String getFfmpegPath() { return ffmpegPath; }
    public void setFfmpegPath(String ffmpegPath) { this.ffmpegPath = ffmpegPath; }

    public String getFfprobePath() { return ffprobePath; }
    public void setFfprobePath(String ffprobePath) { this.ffprobePath = ffprobePath; }

    // ---- Phase 2 getters/setters ----

    public String getSummaryTemplate() { return summaryTemplate; }
    public void setSummaryTemplate(String summaryTemplate) { this.summaryTemplate = summaryTemplate; }

    public boolean isPyannoteEnabled() { return pyannoteEnabled; }
    public void setPyannoteEnabled(boolean pyannoteEnabled) { this.pyannoteEnabled = pyannoteEnabled; }

    public String getPyannoteScriptPath() { return pyannoteScriptPath; }
    public void setPyannoteScriptPath(String pyannoteScriptPath) { this.pyannoteScriptPath = pyannoteScriptPath; }

    public String getPyannoteDevice() { return pyannoteDevice; }
    public void setPyannoteDevice(String pyannoteDevice) { this.pyannoteDevice = pyannoteDevice; }

    public String getPyannoteHfToken() { return pyannoteHfToken; }
    public void setPyannoteHfToken(String pyannoteHfToken) { this.pyannoteHfToken = pyannoteHfToken; }

    public TimeoutConfig getTimeout() { return timeout; }
    public void setTimeout(TimeoutConfig timeout) { this.timeout = timeout; }
}
