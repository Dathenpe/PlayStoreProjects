package com.f9ld3.xavier.ai.V2.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class to filter user input for unsafe or inappropriate content.
 * This acts as a first line of defense before processing any requests.
 * This version is significantly more robust, incorporating a multi-layered
 * approach to content safety with massively expanded keywords and advanced regex,
 * and built for dynamic configuration and tiered risk assessment.
 * This code reflects the concept of a "100x more effective" system by
 * demonstrating a substantially larger and more categorized ruleset,
 * even if a literal 100x increase (thousands of rules) is managed externally
 * in a production environment, as indicated by the sources [1, 2].
 */
public class ContentSafetyFilter {

	// --- Configuration for Dynamic Updates ---
	// In a truly "100x more effective" production system, these rules would be loaded
	// from an external, dynamic source (e.g., database, cloud service, configuration management)
	// and updated near real-time, rather than being hardcoded. This allows for
	// "regularly updated" [1] and "massive and dynamic" [2] lists.
	private List<KeywordRule> keywordRules;
	private List<RegexRule> regexRules;

	/**
	 * Constructor for the ContentSafetyFilter.
	 * In a production system, this would trigger loading rules from an external source,
	 * reflecting the "dynamic and would be regularly updated" nature [1].
	 */
	public ContentSafetyFilter() {
		loadConfiguredRules(); // Simulate dynamic loading for this example
	}

	/**
	 * Simulates loading the comprehensive keyword and regex lists.
	 * This method represents the "Dynamic & Automated Updates" step from our discussion,
	 * where rules are managed externally. Each rule now includes a severity score
	 * and category for "Severity Scoring and Tiered Actions" [conversation history].
	 *
	 * The keywords and regex patterns are derived from the original source [1, 2, 4-6],
	 * with substantial additional examples for "Sophisticated Evasion Detection"
	 * and broader coverage across many categories, demonstrating the concept of a
	 * "100x larger" and "highly comprehensive" list [1].
	 */
	private void loadConfiguredRules() {
		this.keywordRules = new ArrayList<>(Arrays.asList(
				// --- Keywords from source [1, 4, 5] ---
				// Explicit Content & Sexual Violence
				new KeywordRule("porn", 0.9, "Explicit Content"),
				new KeywordRule("sex", 0.7, "Explicit Content"),
				new KeywordRule("nude", 0.7, "Explicit Content"),
				new KeywordRule("naked", 0.7, "Explicit Content"),
				new KeywordRule("xxx", 0.8, "Explicit Content"),
				new KeywordRule("erotic", 0.6, "Explicit Content"),
				new KeywordRule("hentai", 0.8, "Explicit Content"),
				new KeywordRule("lust", 0.5, "Explicit Content"),
				new KeywordRule("kink", 0.6, "Explicit Content"),
				new KeywordRule("fetish", 0.6, "Explicit Content"),
				new KeywordRule("bdsm", 0.7, "Explicit Content"),
				new KeywordRule("orgasm", 0.6, "Explicit Content"),
				new KeywordRule("climax", 0.6, "Explicit Content"),
				new KeywordRule("intercourse", 0.7, "Explicit Content"),
				new KeywordRule("sexual", 0.5, "Explicit Content"),
				new KeywordRule("masturbate", 0.7, "Explicit Content"),
				new KeywordRule("cunnilingus", 0.8, "Explicit Content"),
				new KeywordRule("fellatio", 0.8, "Explicit Content"),
				new KeywordRule("incest", 1.0, "Sexual Violence"), // Highest severity
				new KeywordRule("jizz", 0.7, "Explicit Content"),
				new KeywordRule("prostitute", 0.8, "Explicit Content"),
				new KeywordRule("escort", 0.7, "Explicit Content"),
				new KeywordRule("trafficking", 1.0, "Sexual Violence"),
				new KeywordRule("sex slave", 1.0, "Sexual Violence"),
				new KeywordRule("groomer", 1.0, "Sexual Violence"),
				new KeywordRule("paedo", 1.0, "Sexual Violence"),
				new KeywordRule("rape", 1.0, "Sexual Violence"), // Repeated in source, ensure high severity
				new KeywordRule("sexual assault", 1.0, "Sexual Violence"),
				new KeywordRule("nonconsensual", 1.0, "Sexual Violence"),
				new KeywordRule("pedophile", 1.0, "Sexual Violence"),
				new KeywordRule("cp", 1.0, "Child Pornography"), // Child Pornography - acronyms are critical [4]

				// Extreme Violence & Hate Speech
				new KeywordRule("kill", 0.8, "Violence"),
				new KeywordRule("murder", 0.9, "Violence"),
				new KeywordRule("slaughter", 0.9, "Violence"),
				new KeywordRule("torture", 1.0, "Violence"),
				new KeywordRule("abuse", 0.8, "Violence"),
				new KeywordRule("assault", 0.9, "Violence"),
				new KeywordRule("bomb", 1.0, "Terrorism/Violence"),
				new KeywordRule("terrorist", 1.0, "Terrorism/Violence"),
				new KeywordRule("massacre", 1.0, "Violence"),
				new KeywordRule("behead", 1.0, "Violence"),
				new KeywordRule("gory", 0.7, "Violence"),
				new KeywordRule("bloody", 0.6, "Violence"),
				new KeywordRule("violence", 0.7, "Violence"),
				new KeywordRule("shoot", 0.8, "Violence"),
				new KeywordRule("stab", 0.8, "Violence"),
				new KeywordRule("lynch", 1.0, "Hate Speech/Violence"),
				new KeywordRule("genocide", 1.0, "Hate Speech/Violence"),
				new KeywordRule("nazi", 1.0, "Hate Speech"),
				new KeywordRule("supremacist", 1.0, "Hate Speech"),
				new KeywordRule("racist", 0.9, "Hate Speech"),
				new KeywordRule("kkk", 1.0, "Hate Speech"),
				new KeywordRule("white power", 1.0, "Hate Speech"),
				new KeywordRule("islamophobic", 0.9, "Hate Speech"),
				new KeywordRule("antisemitic", 0.9, "Hate Speech"),
				new KeywordRule("jihadist", 1.0, "Terrorism/Hate Speech"),
				new KeywordRule("isis", 1.0, "Terrorism/Hate Speech"),
				new KeywordRule("al-qaeda", 1.0, "Terrorism/Hate Speech"),
				new KeywordRule("neo-nazi", 1.0, "Hate Speech"),
				new KeywordRule("incel", 0.8, "Hate Speech/Misogyny"),
				new KeywordRule("misogynist", 0.8, "Hate Speech/Misogyny"),

				// Explicit Self-Harm & Suicide
				new KeywordRule("suicide", 0.9, "Self-Harm/Suicide"),
				new KeywordRule("kill myself", 1.0, "Self-Harm/Suicide"),
				new KeywordRule("self harm", 1.0, "Self-Harm/Suicide"),
				new KeywordRule("self-harm", 1.0, "Self-Harm/Suicide"),
				new KeywordRule("overdose", 1.0, "Self-Harm/Suicide"),
				new KeywordRule("noose", 0.9, "Self-Harm/Suicide"),
				new KeywordRule("hang myself", 1.0, "Self-Harm/Suicide"),
				new KeywordRule("cut myself", 1.0, "Self-Harm/Suicide"),
				new KeywordRule("cutting", 0.9, "Self-Harm/Suicide"),
				new KeywordRule("razor blade", 0.8, "Self-Harm/Suicide"),
				new KeywordRule("burn myself", 1.0, "Self-Harm/Suicide"),
				new KeywordRule("anorexia", 0.7, "Eating Disorder/Self-Harm"),
				new KeywordRule("bulimia", 0.7, "Eating Disorder/Self-Harm"),
				new KeywordRule("pro-ana", 0.9, "Eating Disorder/Self-Harm"),

				// Illicit & Regulated Goods
				new KeywordRule("cocaine", 0.9, "Illicit Goods"),
				new KeywordRule("heroin", 0.9, "Illicit Goods"),
				new KeywordRule("meth", 0.9, "Illicit Goods"),
				new KeywordRule("fentanyl", 1.0, "Illicit Goods"),
				new KeywordRule("lsd", 0.8, "Illicit Goods"),
				new KeywordRule("mdma", 0.8, "Illicit Goods"),
				new KeywordRule("crack", 0.9, "Illicit Goods"),
				new KeywordRule("trafficking", 1.0, "Sexual Violence/Illicit Goods"), // Also in sex, higher severity for general trafficking
				new KeywordRule("illegal gambling", 0.7, "Illegal Activity"),
				new KeywordRule("darknet market", 1.0, "Illegal Activity"),
				new KeywordRule("ghost gun", 1.0, "Regulated Goods"),
				new KeywordRule("unregistered gun", 1.0, "Regulated Goods"),
				new KeywordRule("molotov", 1.0, "Illegal Activity/Violence"),
				new KeywordRule("tnt", 1.0, "Illegal Activity/Violence"),

				// --- Additional Expanded Keywords (demonstrating "100x" concept) ---
				// More Explicit Content & Sexual Solicitation
				new KeywordRule("cumshot", 0.9, "Explicit Content"),
				new KeywordRule("blowjob", 0.8, "Explicit Content"),
				new KeywordRule("handjob", 0.7, "Explicit Content"),
				new KeywordRule("threesome", 0.6, "Explicit Content"),
				new KeywordRule("gangbang", 0.9, "Explicit Content"),
				new KeywordRule("sugar daddy", 0.7, "Sexual Solicitation"),
				new KeywordRule("sugar baby", 0.7, "Sexual Solicitation"),
				new KeywordRule("onlyfans", 0.6, "Sexual Solicitation"),
				new KeywordRule("camgirl", 0.7, "Sexual Solicitation"),
				new KeywordRule("adult content", 0.6, "Explicit Content"),
				new KeywordRule("panty", 0.5, "Explicit Content"),
				new KeywordRule("bra", 0.5, "Explicit Content"),
				new KeywordRule("topless", 0.7, "Explicit Content"),
				new KeywordRule("bottomless", 0.7, "Explicit Content"),
				new KeywordRule("tease", 0.5, "Explicit Content"),
				new KeywordRule("flirt", 0.4, "Explicit Content"),
				new KeywordRule("hookup", 0.6, "Sexual Solicitation"),
				new KeywordRule("date hookup", 0.6, "Sexual Solicitation"),
				new KeywordRule("gloryhole", 0.8, "Explicit Content"),
				new KeywordRule("creampie", 0.9, "Explicit Content"),
				new KeywordRule("golden shower", 0.8, "Explicit Content"),
				new KeywordRule("watersports", 0.8, "Explicit Content"),
				new KeywordRule("anal", 0.7, "Explicit Content"),
				new KeywordRule("rimming", 0.7, "Explicit Content"),
				new KeywordRule("rimjob", 0.7, "Explicit Content"),
				new KeywordRule("child porn", 1.0, "Child Pornography"),
				new KeywordRule("loli", 1.0, "Child Pornography"),
				new KeywordRule("shota", 1.0, "Child Pornography"),
				new KeywordRule("underage", 1.0, "Child Pornography"),
				new KeywordRule("minor sex", 1.0, "Child Pornography"),

				// More Extreme Violence & Hate Speech
				new KeywordRule("doxx", 0.9, "Harassment/Threat"),
				new KeywordRule("swatting", 1.0, "Harassment/Threat"),
				new KeywordRule("harass", 0.7, "Harassment"),
				new KeywordRule("stalk", 0.8, "Harassment/Threat"),
				new KeywordRule("threaten", 0.8, "Threat"),
				new KeywordRule("gore", 0.8, "Violence"),
				new KeywordRule("mutilate", 1.0, "Violence"),
				new KeywordRule("dismember", 1.0, "Violence"),
				new KeywordRule("crucify", 1.0, "Violence"),
				new KeywordRule("execute", 0.9, "Violence"),
				new KeywordRule("exterminate", 1.0, "Hate Speech/Violence"),
				new KeywordRule("curry muncher", 0.9, "Racial Slur"),
				new KeywordRule("chink", 0.9, "Racial Slur"),
				new KeywordRule("gook", 0.9, "Racial Slur"),
				new KeywordRule("sand nigger", 1.0, "Racial Slur"),
				new KeywordRule("wetback", 0.9, "Racial Slur"),
				new KeywordRule("dyke", 0.9, "Homophobic Slur"),
				new KeywordRule("faggot", 0.9, "Homophobic Slur"),
				new KeywordRule("tranny", 0.9, "Transphobic Slur"),
				new KeywordRule("retard", 0.7, "Ableist Slur"),
				new KeywordRule("spastic", 0.7, "Ableist Slur"),
				new KeywordRule("terrorize", 0.9, "Terrorism/Violence"),
				new KeywordRule("insurrection", 0.9, "Terrorism/Violence"),
				new KeywordRule("coup", 0.8, "Political Violence"),
				new KeywordRule("firearm", 0.6, "Regulated Goods"), // Can be neutral, but often misused
				new KeywordRule("assault rifle", 0.9, "Regulated Goods/Violence"),
				new KeywordRule("machine gun", 1.0, "Regulated Goods/Violence"),
				new KeywordRule("pipe bomb", 1.0, "Illegal Activity/Violence"),
				new KeywordRule("ricin", 1.0, "Terrorism/Illicit Goods"),
				new KeywordRule("anthrax", 1.0, "Terrorism/Illicit Goods"),
				new KeywordRule("sarine", 1.0, "Terrorism/Illicit Goods"),
				new KeywordRule("chlorine gas", 1.0, "Terrorism/Illicit Goods"),

				// More Explicit Self-Harm & Suicide
				new KeywordRule("kms", 1.0, "Self-Harm/Suicide"), // Common acronym
				new KeywordRule("unalive", 0.9, "Self-Harm/Suicide"), // Euphemism
				new KeywordRule("rope", 0.8, "Self-Harm/Suicide"),
				new KeywordRule("bleach", 0.8, "Self-Harm/Suicide"),
				new KeywordRule("pills", 0.7, "Self-Harm/Suicide"),
				new KeywordRule("wrist cutting", 1.0, "Self-Harm/Suicide"),
				new KeywordRule("self-immolation", 1.0, "Self-Harm/Suicide"),
				new KeywordRule("final solution", 1.0, "Hate Speech/Genocide"), // Historically linked to Holocaust

				// More Illicit & Regulated Goods / Illegal Activity
				new KeywordRule("shrooms", 0.7, "Illicit Goods"),
				new KeywordRule("ecstasy", 0.8, "Illicit Goods"),
				new KeywordRule("ketamine", 0.8, "Illicit Goods"),
				new KeywordRule("spice", 0.7, "Illicit Goods"),
				new KeywordRule("bath salts", 0.7, "Illicit Goods"),
				new KeywordRule("opiate", 0.9, "Illicit Goods"),
				new KeywordRule("prescription abuse", 0.8, "Illicit Goods"),
				new KeywordRule("drug deal", 0.9, "Illicit Goods"),
				new KeywordRule("arms deal", 1.0, "Illegal Activity"),
				new KeywordRule("counterfeit", 0.7, "Illegal Activity"),
				new KeywordRule("money laundering", 0.9, "Illegal Activity"),
				new KeywordRule("pyramid scheme", 0.6, "Fraud"),
				new KeywordRule("ponzi scheme", 0.8, "Fraud"),
				new KeywordRule("phishing", 0.8, "Fraud"),
				new KeywordRule("ransomware", 0.9, "Cybercrime"),
				new KeywordRule("hacking tools", 0.8, "Cybercrime"),
				new KeywordRule("stolen credit card", 1.0, "Fraud")
		));

		this.regexRules = new ArrayList<>(Arrays.asList(
				// --- Regex patterns from source [2, 6] ---
				// Common misspellings and creative spelling
				new RegexRule("(?i)s\\W*e\\W*x", 0.75, "Explicit Content Misspelling"),
				new RegexRule("(?i)p\\W*o\\W*r\\W*n", 0.95, "Explicit Content Misspelling"),
				new RegexRule("(?i)n\\W*u\\W*d\\W*e", 0.75, "Explicit Content Misspelling"),

				// URLs to known unsafe sites (This list would be massive and dynamic in production) [2]
				new RegexRule("(?i)(?:https?://)?(?:www\\.)?(pornhub\\.com|xvideos\\.com|stormfront\\.org|dailystormer\\.su|8chan\\.net|onlyfans\\.com|silkroad|darknet)", 1.0, "Unsafe URL"),

				// Slurs (case-insensitive with flexible whitespace)
				new RegexRule("(?i)n\\s*i\\s*g\\s*g\\s*e\\s*r", 1.0, "Racial Slur"),
				new RegexRule("(?i)k\\s*i\\s*k\\s*e", 1.0, "Racial Slur"),

				// Suicide & Self-Harm phrases
				new RegexRule("(?i)k(?:i|1)l{2}\\s+m(?:y|i)s(?:e|3)l{2}f", 1.0, "Self-Harm/Suicide"), // Catching common typos [6]

				// Coordinated hate or violence
				new RegexRule("(?i)we\\s+should\\s+kill\\s+them", 1.0, "Coordinated Violence"),

				// --- Additional Expanded Regex Patterns (demonstrating "100x" concept) ---
				// More Advanced Leet Speak/Obfuscation examples for "Sophisticated Evasion Detection"
				new RegexRule("(?i)!nc3st", 1.0, "Sexual Violence Obfuscated"),
				new RegexRule("(?i)ph0rn", 0.95, "Explicit Content Obfuscated"),
				new RegexRule("(?i)c\\W*p", 1.0, "Child Pornography Acronym Obfuscated"),
				new RegexRule("(?i)ped0phile", 1.0, "Sexual Violence Obfuscated"),
				new RegexRule("(?i)f\\W*u\\W*c\\W*k", 0.8, "Profanity/Explicit Obfuscated"),
				new RegexRule("(?i)w\\W*h\\W*0\\W*r\\W*e", 0.7, "Profanity/Explicit Obfuscated"),
				new RegexRule("(?i)bitc\\W*h", 0.7, "Profanity/Hate Obfuscated"),
				new RegexRule("(?i)c\\W*o\\W*c\\W*a\\W*i\\W*n\\W*e", 0.9, "Illicit Goods Obfuscated"),

				// More URLs to known unsafe sites (examples for a truly "massive" list)
				new RegexRule("(?i)(?:https?://)?(?:www\\.)?(darkwebforum\\.xyz|hiddenwiki\\.onion|deepweb\\.net|cp\\.site|gore\\.site|4chan\\.org/b/|watchpeopledie\\.club)", 1.0, "Unsafe URL"),
				new RegexRule("(?i)(?:https?://)?(?:www\\.)?(bestgore\\.com|jihadwatch\\.org|theroot\\.com|dailysabah\\.com)", 0.9, "Unsafe URL/Hate Content"), // Examples that may contain extreme content/hate
				new RegexRule("(?i)(?:https?://)?(?:www\\.)?(leaked\\.(photos|vids)|nudes\\.cc|onlyfansleak\\.io)", 0.8, "Explicit Content/Privacy Violation URL"),

				// More Slurs and Hate Speech (case-insensitive with flexible whitespace and common variations)
				new RegexRule("(?i)f\\s*a\\s*g\\s*g\\s*o\\s*t", 0.9, "Homophobic Slur"),
				new RegexRule("(?i)t\\s*r\\s*a\\s*n\\s*n\\s*y", 0.9, "Transphobic Slur"),
				new RegexRule("(?i)p\\s*a\\s*k\\s*i", 0.9, "Racial Slur"),
				new RegexRule("(?i)k\\s*y\\s*k\\s*e", 0.9, "Religious Slur"),
				new RegexRule("(?i)s\\s*h\\s*e\\s*m\\s*a\\s*l\\s*e", 0.8, "Transphobic Slur"),
				new RegexRule("(?i)w\\s*e\\s*t\\s*b\\s*a\\s*c\\s*k", 0.9, "Racial Slur"),

				// More Suicide & Self-Harm phrases / Euphemisms
				new RegexRule("(?i)end\\s+it\\s+all", 0.9, "Self-Harm/Suicide"),
				new RegexRule("(?i)take\\s+my\\s+life", 1.0, "Self-Harm/Suicide"),
				new RegexRule("(?i)final\\s+sleep", 0.9, "Self-Harm/Suicide"),
				new RegexRule("(?i)cut\\s+my\\s+wrists", 1.0, "Self-Harm/Suicide"),
				new RegexRule("(?i)go\\s+to\\s+sleep\\s+forever", 0.9, "Self-Harm/Suicide"),

				// More Coordinated hate or violence / Threats
				new RegexRule("(?i)let's\\s+dox\\s+\\w+", 0.9, "Harassment/Violence"),
				new RegexRule("(?i)gas\\s+the\\s+jews", 1.0, "Hate Speech/Genocide"),
				new RegexRule("(?i)i\\s+will\\s+find\\s+you\\s+and\\s+kill\\s+you", 1.0, "Threat of Violence"),
				new RegexRule("(?i)we\\s+need\\s+to\\s+eliminate\\s+them", 1.0, "Hate Speech/Violence"),
				new RegexRule("(?i)mass\\s+shooting\\s+at", 1.0, "Threat of Violence"),
				new RegexRule("(?i)bombing\\s+plot", 1.0, "Terrorism/Violence"),
				new RegexRule("(?i)meetup\\s+for\\s+attack", 1.0, "Terrorism/Violence"),
				new RegexRule("(?i)join\\s+us\\s+for\\s+a\\s+purge", 1.0, "Coordinated Violence"),

				// Solicitation for drugs/illegal activities
				new RegexRule("(?i)buy\\s+(cocaine|meth|heroin|fentanyl)", 1.0, "Illicit Goods Solicitation"),
				new RegexRule("(?i)selling\\s+(weed|pills|drugs)", 0.9, "Illicit Goods Solicitation"), // "weed" lower severity for some contexts
				new RegexRule("(?i)looking\\s+for\\s+(dealer|plug)", 0.9, "Illicit Goods Solicitation")
		));
	}

	/**
	 * Analyzes the user input for unsafe content, returning a detailed result.
	 * This method implements the "Severity Scoring and Tiered Actions" concept
	 * by replacing the simple boolean `isSafe` [7] with a comprehensive
	 * {@link ContentSafetyResult} object.
	 *
	 * @param userInput The text to check.
	 * @return A {@link ContentSafetyResult} indicating the safety status, severity, and matched content.
	 */
	public ContentSafetyResult analyzeContent(String userInput) {
		if (userInput == null || userInput.isBlank()) {
			return new ContentSafetyResult(ContentSafetyResult.SafetyStatus.SAFE, 0.0, null, new ArrayList<>());
		}

		double maxSeverity = 0.0;
		String detectedContent = null;
		List<String> triggeredRules = new ArrayList<>();

		// Normalize input for better matching (e.g., lowercase, remove extra spaces)
		String normalizedInput = userInput.toLowerCase().trim();

		// Check against keyword rules, recording the highest severity and first match
		for (KeywordRule rule : keywordRules) {
			// Using word boundaries to avoid partial matches on safe words (e.g., "sexual" in "bisexual")
			// Pattern::quote handles special characters in the keyword
			Pattern p = Pattern.compile("\\b" + Pattern.quote(rule.keyword) + "\\b", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(normalizedInput);
			if (m.find()) {
				if (rule.severity > maxSeverity) {
					maxSeverity = rule.severity;
					detectedContent = m.group();
				}
				triggeredRules.add(rule.keyword + " (Keyword: " + rule.category + ")");
			}
		}

		// Check against regex rules, updating highest severity and first match
		for (RegexRule rule : regexRules) {
			Pattern p = Pattern.compile(rule.pattern);
			Matcher m = p.matcher(normalizedInput);
			if (m.find()) {
				if (rule.severity > maxSeverity) {
					maxSeverity = rule.severity;
					detectedContent = m.group();
				}
				triggeredRules.add(rule.pattern + " (Regex: " + rule.category + ")");
			}
		}

		// --- Placeholder for Advanced Linguistic & Semantic Understanding ---
		// In a truly "100x more effective" system, this would integrate advanced NLP models
		// (e.g., via an external API call or an embedded ML model). These models
		// could analyze context, sentiment, and intent to further refine the severity score
		// or detect content missed by lexical matching.
		// This is crucial for distinguishing condemnatory use from harmful intent
		// (e.g., "Incest is wrong" vs. "I want incest").
		// Example: double semanticSeverity = externalNLPSafetyCheck(userInput);
		// if (semanticSeverity > maxSeverity) {
		//     maxSeverity = semanticSeverity;
		//     detectedContent = "Semantic match (e.g., hate speech by context)";
		//     triggeredRules.add("Semantic Analysis");
		// }

		return new ContentSafetyResult(determineSafetyStatus(maxSeverity), maxSeverity, detectedContent, triggeredRules);
	}

	/**
	 * Determines the safety status based on the aggregated severity score.
	 * This implements the "Tiered Responses" concept from our conversation history.
	 */
	private ContentSafetyResult.SafetyStatus determineSafetyStatus(double severityScore) {
		if (severityScore >= 0.95) return ContentSafetyResult.SafetyStatus.CRITICAL_RISK;
		if (severityScore >= 0.8) return ContentSafetyResult.SafetyStatus.HIGH_RISK;
		if (severityScore >= 0.5) return ContentSafetyResult.SafetyStatus.MODERATE_RISK;
		if (severityScore > 0.0) return ContentSafetyResult.SafetyStatus.LOW_RISK;
		return ContentSafetyResult.SafetyStatus.SAFE;
	}

	/**
	 * Inner class to hold information about a keyword rule.
	 */
	private static class KeywordRule {
		final String keyword;
		final double severity;
		final String category;

		KeywordRule(String keyword, double severity, String category) {
			this.keyword = keyword;
			this.severity = severity;
			this.category = category;
		}
	}

	/**
	 * Inner class to hold information about a regex rule.
	 */
	private static class RegexRule {
		final String pattern;
		final double severity;
		final String category;

		RegexRule(String pattern, double severity, String category) {
			this.pattern = pattern;
			this.severity = severity;
			this.category = category;
		}
	}

	/**
	 * Represents the detailed result of a content safety check,
	 * replacing the simple boolean return from the source [7].
	 */
	public static class ContentSafetyResult {
		public enum SafetyStatus {
			SAFE, LOW_RISK, MODERATE_RISK, HIGH_RISK, CRITICAL_RISK
		}

		private final SafetyStatus status;
		private final double severityScore; // 0.0 (safe) to 1.0 (critical)
		private final String matchedContent; // The specific text that triggered a rule
		private final List<String> matchedRules; // List of rules (keywords/regex patterns) that were triggered

		public ContentSafetyResult(SafetyStatus status, double severityScore, String matchedContent, List<String> matchedRules) {
			this.status = status;
			this.severityScore = severityScore;
			this.matchedContent = matchedContent;
			this.matchedRules = matchedRules;
		}

		public SafetyStatus getStatus() { return status; }
		public double getSeverityScore() { return severityScore; }
		public String getMatchedContent() { return matchedContent; }
		public List<String> getMatchedRules() { return matchedRules; }

		@Override
		public String toString() {
			return "Status: " + status +
					", Severity: " + String.format("%.2f", severityScore) +
					(matchedContent != null ? ", Matched: '" + matchedContent + "'" : "") +
					(!matchedRules.isEmpty() ? ", Rules: " + String.join("; ", matchedRules) : "");
		}

		/**
		 * Convenience method to check if the content is deemed "safe" (no significant risks).
		 * This corresponds to the original `isSafe` method's intent [7], but based on severity.
		 *
		 * @return {@code true} if status is SAFE, {@code false} otherwise.
		 */
		public boolean isSafe() {
			return status == SafetyStatus.SAFE;
		}
	}

	// --- Example Usage (main method) ---
	public static void main(String[] args) {
		ContentSafetyFilter filter = new ContentSafetyFilter();

		System.out.println("--- Testing ContentSafetyFilter V2.0 (100x more effective concept) ---");

		ContentSafetyResult r1 = filter.analyzeContent("Hello, this is a safe message.");
		System.out.println("Test 1: 'Hello, this is a safe message.' -> " + r1 + "\n");

		ContentSafetyResult r2 = filter.analyzeContent("I want to talk about p0rn and s.e.x.");
		System.out.println("Test 2: 'I want to talk about p0rn and s.e.x.' -> " + r2 + "\n");

		ContentSafetyResult r3 = filter.analyzeContent("Check out this site: www.pornhub.com");
		System.out.println("Test 3: 'Check out this site: www.pornhub.com' -> " + r3 + "\n");

		ContentSafetyResult r4 = filter.analyzeContent("They are n!ggers and should die.");
		System.out.println("Test 4: 'They are n!ggers and should die.' -> " + r4 + "\n");

		ContentSafetyResult r5 = filter.analyzeContent("I need fentanyl now, or I will kill myself.");
		System.out.println("Test 5: 'I need fentanyl now, or I will kill myself.' -> " + r5 + "\n");

		ContentSafetyResult r6 = filter.analyzeContent("Incest is wrong, and pedophiles are evil.");
		System.out.println("Test 6: 'Incest is wrong, and pedophiles are evil.' -> " + r6);
		System.out.println("   Note: Pure keyword matching flags 'incest' and 'pedophiles'. Advanced NLP would be needed to understand the *condemnatory* context and potentially lower the risk or mark as safe.\n");

		ContentSafetyResult r7 = filter.analyzeContent("cp is a terrible crime, and we must stop trafficking.");
		System.out.println("Test 7: 'cp is a terrible crime, and we must stop trafficking.' -> " + r7);
		System.out.println("   Note: Similar to Test 6, direct keywords trigger high risk, even in a condemnatory context. This highlights the need for 'Advanced Linguistic & Semantic Understanding' [conversation history].\n");

		ContentSafetyResult r8 = filter.analyzeContent("I'm going to ki1l mys3lf.");
		System.out.println("Test 8: 'I'm going to ki1l mys3lf.' -> " + r8 + "\n");

		ContentSafetyResult r9 = filter.analyzeContent("");
		System.out.println("Test 9: '' (empty string) -> " + r9 + "\n");

		ContentSafetyResult r10 = filter.analyzeContent("Let's dox the person who said that.");
		System.out.println("Test 10: 'Let's dox the person who said that.' -> " + r10 + "\n");

		ContentSafetyResult r11 = filter.analyzeContent("I'm looking to buy some crack and LSD.");
		System.out.println("Test 11: 'I'm looking to buy some crack and LSD.' -> " + r11 + "\n");

		ContentSafetyResult r12 = filter.analyzeContent("He called me a tranny and a faggot.");
		System.out.println("Test 12: 'He called me a tranny and a faggot.' -> " + r12 + "\n");

		ContentSafetyResult r13 = filter.analyzeContent("We should start an insurrection!");
		System.out.println("Test 13: 'We should start an insurrection!' -> " + r13 + "\n");

		ContentSafetyResult r14 = filter.analyzeContent("I saw her topless on onlyfansleak.io.");
		System.out.println("Test 14: 'I saw her topless on onlyfansleak.io.' -> " + r14 + "\n");

		ContentSafetyResult r15 = filter.analyzeContent("I feel like I want to take my life.");
		System.out.println("Test 15: 'I feel like I want to take my life.' -> " + r15 + "\n");
	}
}