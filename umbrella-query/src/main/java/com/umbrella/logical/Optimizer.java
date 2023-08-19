package com.umbrella.logical;

import org.apache.calcite.adapter.arrow.ArrowSchema;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.plan.*;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.sql2rel.StandardConvertletTable;
import org.apache.calcite.tools.Program;
import org.apache.calcite.tools.Programs;
import org.apache.calcite.tools.RuleSet;
import org.apache.calcite.tools.RuleSets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Optimizer {
    private final CalciteConnectionConfig config;
    private final SqlValidator validator;
    private final SqlToRelConverter converter;
    private final RelOptPlanner planner;

    public Optimizer(CalciteConnectionConfig config, SqlValidator validator, SqlToRelConverter converter, RelOptPlanner planner) {
        this.config = config;
        this.validator = validator;
        this.converter = converter;
        this.planner = planner;
    }

    public static Optimizer create(ArrowSchema schema, Collection<RelOptRule> rules) {
        var configProperties = new Properties();
        configProperties.put(CalciteConnectionProperty.CASE_SENSITIVE.camelName(), Boolean.TRUE.toString());
        configProperties.put(CalciteConnectionProperty.UNQUOTED_CASING.camelName(), Casing.UNCHANGED.toString());
        configProperties.put(CalciteConnectionProperty.QUOTED_CASING.camelName(), Casing.UNCHANGED.toString());
        var config = new CalciteConnectionConfigImpl(configProperties);

        // create root schema
        var rootSchema = CalciteSchema.createRootSchema(false, false);
        rootSchema.add(schema.getName(), schema);

        var typeFactory = new JavaTypeFactoryImpl();

        // create catalog reader, needed by SqlValidator
        var catalogReader = new CalciteCatalogReader(
                rootSchema,
                Collections.singletonList(schema.getName()),
                typeFactory,
                config);

        // create SqlValidator
        var validatorConfig = SqlValidator.Config.DEFAULT
                .withLenientOperatorLookup(config.lenientOperatorLookup())
                .withConformance(config.conformance())
                .withDefaultNullCollation(config.defaultNullCollation())
                .withIdentifierExpansion(true);
        var validator = SqlValidatorUtil.newValidator(
                SqlStdOperatorTable.instance(), catalogReader, typeFactory, validatorConfig);


        var program = new HepProgramBuilder().addRuleCollection(rules).build();
        var planner = new HepPlanner(program);

        planner.addRelTraitDef(ConventionTraitDef.INSTANCE);

        // create SqlToRelConverter
        var cluster = RelOptCluster.create(planner, new RexBuilder(typeFactory));
        var converterConfig = SqlToRelConverter.config()
                .withTrimUnusedFields(true)
                .withExpand(false);
        var converter = new SqlToRelConverter(
                null,
                validator,
                catalogReader,
                cluster,
                StandardConvertletTable.INSTANCE,
                converterConfig);

        return new Optimizer(config, validator, converter, planner);
    }

    public SqlNode parse(String sql) throws SqlParseException {
        var parserConfig = SqlParser.config()
                .withQuotedCasing(config.quotedCasing())
                .withUnquotedCasing(config.unquotedCasing())
                .withQuoting(config.quoting())
                .withConformance(config.conformance())
                .withCaseSensitive(config.caseSensitive());
        SqlParser parser = SqlParser.create(sql, parserConfig);

        return parser.parseStmt();
    }

    public SqlNode validate(SqlNode node) {
        return validator.validate(node);
    }

    public RelNode convert(SqlNode node) {
        var root = converter.convertQuery(node, false, true);
        return root.rel;
    }

    public RelNode optimize(RelNode node, RelTraitSet requiredTraitSet) {
        Program program = Programs.ofRules();
        return program.run(
                planner,
                node,
                requiredTraitSet,
                Collections.emptyList(),
                Collections.emptyList());
    }
}
