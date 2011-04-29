/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test;

import org.hibernate.cfg.Environment;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.event.EnversIntegrator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.junit.Before;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractEntityTest extends AbstractEnversTest {
    private EntityManagerFactory emf;
    private EntityManager entityManager;
    private AuditReader auditReader;
    private Ejb3Configuration cfg;
	private BasicServiceRegistryImpl serviceRegistry;
    private boolean audited;

    public abstract void configure(Ejb3Configuration cfg);

    public void addConfigurationProperties(Properties configuration) { }

    private void closeEntityManager() {
        if (entityManager != null) {
            entityManager.close();
            entityManager = null;
        }
    }

    @Before
    public void newEntityManager() {
        closeEntityManager();
        
        entityManager = emf.createEntityManager();

        if (audited) {
            auditReader = AuditReaderFactory.get(entityManager);
        }
    }

    @BeforeClassOnce
    public void init() throws IOException {
        init(true, getAuditStrategy());
    }

    protected void init(boolean audited, String auditStrategy) throws IOException {
        this.audited = audited;

        Properties configurationProperties = new Properties();
        if (!audited) {
			configurationProperties.setProperty(EnversIntegrator.AUTO_REGISTER, "false");
        }

        configurationProperties.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
        configurationProperties.setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
        configurationProperties.setProperty(Environment.DRIVER, "org.h2.Driver");
        configurationProperties.setProperty(Environment.USER, "sa");

        // Separate database for each test class
        configurationProperties.setProperty(Environment.URL, "jdbc:h2:mem:" + this.getClass().getName() + ";DB_CLOSE_DELAY=-1");

        if (auditStrategy != null && !"".equals(auditStrategy)) {
            configurationProperties.setProperty("org.hibernate.envers.audit_strategy", auditStrategy);
        }

        addConfigurationProperties(configurationProperties);

        cfg = new Ejb3Configuration();
        configure(cfg);
        cfg.configure(configurationProperties);

        serviceRegistry = createServiceRegistry(cfg);

        emf = cfg.buildEntityManagerFactory( serviceRegistry );

        newEntityManager();
    }

    private BasicServiceRegistryImpl createServiceRegistry(Ejb3Configuration configuration) {
        Properties properties = new Properties();
		properties.putAll(configuration.getHibernateConfiguration().getProperties());
		ConfigurationHelper.resolvePlaceHolders(properties);
		return (BasicServiceRegistryImpl) new ServiceRegistryBuilder(properties).buildServiceRegistry();
    }

    @AfterClassOnce
    public void close() {
        closeEntityManager();
        emf.close();
		serviceRegistry.destroy();
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public AuditReader getAuditReader() {
        return auditReader;
    }

    public Ejb3Configuration getCfg() {
        return cfg;
    }
}
