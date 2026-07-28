package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Hashtable;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w-]+(\\.[\\w-]+)*\\.[a-z]{2,}$");
    private final DnsRecordResolver dnsRecordResolver;

    public ValidEmailValidator() {
        this(new JndiDnsRecordResolver());
    }

    ValidEmailValidator(DnsRecordResolver dnsRecordResolver) {
        this.dnsRecordResolver = dnsRecordResolver;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            return false;
        }

        String domain = value.substring(value.lastIndexOf('@') + 1);
        return dnsRecordResolver.hasMailOrAddressRecord(domain);
    }

    @FunctionalInterface
    interface DnsRecordResolver {
        boolean hasMailOrAddressRecord(String domain);
    }

    private static class JndiDnsRecordResolver implements DnsRecordResolver {

        @Override
        public boolean hasMailOrAddressRecord(String domain) {
            return hasRecord(domain, "MX") || hasRecord(domain, "A");
        }

        private boolean hasRecord(String domain, String recordType) {
            Hashtable<String, String> environment = new Hashtable<>();
            environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            environment.put("com.sun.jndi.dns.timeout.initial", "2000");
            environment.put("com.sun.jndi.dns.timeout.retries", "1");

            DirContext context = null;
            try {
                context = new InitialDirContext(environment);
                Attributes attributes = context.getAttributes(domain, new String[] {recordType});
                Attribute records = attributes.get(recordType);
                return records != null && records.size() > 0;
            } catch (NamingException ex) {
                return false;
            } finally {
                if (context != null) {
                    try {
                        context.close();
                    } catch (NamingException ex) {
                        // DNS validation already completed; close failures do not change validity.
                    }
                }
            }
        }
    }
}
