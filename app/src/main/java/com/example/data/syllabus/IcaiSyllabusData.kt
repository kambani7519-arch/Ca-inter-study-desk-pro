package com.example.data.syllabus

import com.example.data.model.GroupType
import com.example.data.model.PaperInfo
import com.example.data.model.TopicItem
import com.example.data.model.TopicStatus

object IcaiSyllabusData {

  val papers = listOf(
    PaperInfo(
      id = 1,
      code = "PAPER-1",
      name = "Advanced Accounting",
      group = GroupType.GROUP_1,
      iconName = "account_balance",
      description = "Accounting Standards, Consolidated FS, Amalgamation, Buyback, Branch Accounts",
      chaptersCount = 7
    ),
    PaperInfo(
      id = 2,
      code = "PAPER-2",
      name = "Corporate & Other Laws",
      group = GroupType.GROUP_1,
      iconName = "gavel",
      description = "Companies Act 2013 (Ch 1-10 & Foreign Cos), LLP Act, FEMA, General Clauses, Interpretation",
      chaptersCount = 5
    ),
    PaperInfo(
      id = 3,
      code = "PAPER-3",
      name = "Taxation (DT & GST)",
      group = GroupType.GROUP_1,
      iconName = "receipt_long",
      description = "Direct Tax (5 Heads, Total Income, TDS/TCS) and Indirect Tax (GST, ITC, Supply, Returns)",
      chaptersCount = 14
    ),
    PaperInfo(
      id = 4,
      code = "PAPER-4",
      name = "Cost & Management Accounting",
      group = GroupType.GROUP_2,
      iconName = "calculate",
      description = "Material, Labour, Overheads, ABC, Cost Sheet, Marginal, Standard Costing, Budgets",
      chaptersCount = 15
    ),
    PaperInfo(
      id = 5,
      code = "PAPER-5",
      name = "Auditing & Ethics",
      group = GroupType.GROUP_2,
      iconName = "verified",
      description = "Standards on Auditing (SAs), Internal Control, Audit Evidence, Audit Reports, CARO 2020 & Ethics",
      chaptersCount = 11
    ),
    PaperInfo(
      id = 6,
      code = "PAPER-6",
      name = "FM & Strategic Management",
      group = GroupType.GROUP_2,
      iconName = "trending_up",
      description = "Financial Management (Ratios, Cost of Capital, Capital Budgeting, WCM) & Strategic Management",
      chaptersCount = 14
    )
  )

  fun getInitialTopics(): List<TopicItem> {
    val list = mutableListOf<TopicItem>()

    // Paper 1: Advanced Accounting
    val p1Chapters = listOf(
      "Accounting Standards (AS 1, 2, 3, 10, 11)" to listOf(
        "AS 1: Disclosure of Accounting Policies" to "AS 1",
        "AS 2: Valuation of Inventories" to "AS 2",
        "AS 3: Cash Flow Statements" to "AS 3",
        "AS 10: Property, Plant and Equipment" to "AS 10",
        "AS 11: Effects of Changes in Foreign Exchange Rates" to "AS 11"
      ),
      "Accounting Standards (AS 12, 13, 16, 19, 20)" to listOf(
        "AS 12: Accounting for Government Grants" to "AS 12",
        "AS 13: Accounting for Investments" to "AS 13",
        "AS 16: Borrowing Costs" to "AS 16",
        "AS 19: Leases (Finance vs Operating)" to "AS 19",
        "AS 20: Earnings Per Share (Basic & Diluted)" to "AS 20"
      ),
      "Accounting Standards (AS 26, 28, 29)" to listOf(
        "AS 26: Intangible Assets" to "AS 26",
        "AS 28: Impairment of Assets" to "AS 28",
        "AS 29: Provisions, Contingent Liabilities & Assets" to "AS 29"
      ),
      "Financial Statements & Buyback of Securities" to listOf(
        "Schedule III Division I Presentation of Balance Sheet & P&L" to "Schedule III",
        "Buyback of Securities & Capital Redemption Reserve (CRR)" to "Sec 68-70",
        "Accounting for Employee Stock Option Plans (ESOP)" to "Guidance Note"
      ),
      "Reconstruction & Amalgamation" to listOf(
        "Internal Reconstruction & Capital Reduction Scheme" to "Sec 66",
        "Amalgamation in the Nature of Merger vs Purchase (AS 14)" to "AS 14",
        "Calculation of Purchase Consideration & Inter-company Balances" to "AS 14"
      ),
      "Consolidated Financial Statements" to listOf(
        "Consolidation of Balance Sheet of Holding & Subsidiary (AS 21)" to "AS 21",
        "Calculation of Minority Interest & Cost of Control / Goodwill" to "AS 21",
        "Elimination of Unrealised Profits & Inter-company Holdings" to "AS 21"
      ),
      "Branch Accounting" to listOf(
        "Dependent Branches (Debtors & Stock/Debtors Method)" to "Branch Acct",
        "Independent Branches & Foreign Branch Incorporation (AS 11)" to "AS 11"
      )
    )

    p1Chapters.forEachIndexed { chIdx, (chName, topics) ->
      topics.forEachIndexed { tIdx, (title, ref) ->
        list.add(
          TopicItem(
            id = "p1_c${chIdx + 1}_t${tIdx + 1}",
            paperId = 1,
            chapterName = chName,
            topicTitle = title,
            icaiRef = ref,
            status = TopicStatus.PENDING
          )
        )
      }
    }

    // Paper 2: Corporate & Other Laws
    val p2Chapters = listOf(
      "Preliminary & Incorporation of Company" to listOf(
        "Types of Companies & Lifting of Corporate Veil" to "Sec 2(20), 2(68), 2(71)",
        "Memorandum & Articles of Association (Ultra Vires & Indoor Mgmt)" to "Sec 4-6",
        "Registered Office, Conversion & Commencing Business" to "Sec 10A, 12"
      ),
      "Prospectus, Capital & Deposits" to listOf(
        "Shelf, Red-Herring Prospectus & Civil/Criminal Liability" to "Sec 25-38",
        "Issue of Shares at Premium / Discount, Sweat Equity & Rights Issue" to "Sec 52-62",
        "Acceptance of Deposits from Members & Public" to "Sec 73-76A",
        "Registration, Modification & Satisfaction of Charges" to "Sec 77-87"
      ),
      "Management, Dividend & Accounts" to listOf(
        "Annual General Meeting, Notice, Quorum & Proxies" to "Sec 96-105",
        "Resolutions, Voting Rights & Postal Ballot" to "Sec 106-121",
        "Declaration & Payment of Dividend, IEPF Transfer" to "Sec 123-127",
        "Books of Accounts, Internal Audit & CSR Provisions" to "Sec 128, 135, 138"
      ),
      "Audit, Auditors & Foreign Companies" to listOf(
        "Appointment, Rotation & Removal of Statutory Auditors" to "Sec 139-140",
        "Eligibility, Disqualifications & Powers/Duties of Auditors" to "Sec 141, 143",
        "Companies Incorporated Outside India (Applicability & Accounts)" to "Sec 379-393A"
      ),
      "Other Laws (LLP, FEMA, General Clauses, Interpretation)" to listOf(
        "Limited Liability Partnership: Partners, Relations & Winding Up" to "LLP Act 2008",
        "FEMA: Current vs Capital Account Transactions & Realisation" to "FEMA 1999 Sec 5-8",
        "General Clauses Act: Definitions, Computation of Time & Repeals" to "GCA 1897",
        "Interpretation of Statutes: Primary Rules, Internal & External Aids" to "Interpretation"
      )
    )

    p2Chapters.forEachIndexed { chIdx, (chName, topics) ->
      topics.forEachIndexed { tIdx, (title, ref) ->
        list.add(
          TopicItem(
            id = "p2_c${chIdx + 1}_t${tIdx + 1}",
            paperId = 2,
            chapterName = chName,
            topicTitle = title,
            icaiRef = ref,
            status = TopicStatus.PENDING
          )
        )
      }
    }

    // Paper 3: Taxation (DT & GST)
    val p3Chapters = listOf(
      "Basic Concepts & Residential Status (DT)" to listOf(
        "Scope of Total Income & Residential Status Criteria" to "Sec 5 & 6",
        "New Concessional Tax Regime vs Normal Scheme" to "Sec 115BAC",
        "Agricultural Income & Partial Integration" to "Sec 10(1)"
      ),
      "Heads of Income: Salary & House Property (DT)" to listOf(
        "Salary Allowances, Perquisites Valuation & Deductions" to "Sec 15-17",
        "House Property NAV, Interest on Borrowed Capital u/s 24(b)" to "Sec 22-27"
      ),
      "Profits & Gains of Business or Profession (PGBP) (DT)" to listOf(
        "Admissible Deductions (Depreciation u/s 32 & Additional Dep.)" to "Sec 30-37",
        "Disallowed Expenses (Sec 40(a), 40A(2), 40A(3) Cash Limit)" to "Sec 40, 40A",
        "Presumptive Taxation Schemes (44AD, 44ADA, 44AE)" to "Sec 44AD/ADA"
      ),
      "Capital Gains & Other Sources (DT)" to listOf(
        "Capital Asset Definition, Short-term vs Long-term Computation" to "Sec 45, 48",
        "Exemptions u/s 54, 54EC, 54F & Taxation u/s 111A, 112, 112A" to "Sec 54 series",
        "Income from Other Sources & Taxation of Gifts u/s 56(2)(x)" to "Sec 56(2)(x)"
      ),
      "Clubbing, Set-off, Deductions & Filing Returns (DT)" to listOf(
        "Clubbing of Minor's & Spouse's Income" to "Sec 60-64",
        "Inter-head & Intra-head Set-off & Carry Forward of Losses" to "Sec 70-80",
        "Deductions: 80C, 80D (Health), 80G (Donations), 80JJAA" to "Ch VI-A",
        "TDS, TCS, Advance Tax & Due Dates of Filing ITR u/s 139" to "Sec 139, 192-206"
      ),
      "GST: Supply, Charge & Composition Scheme" to listOf(
        "Concept of Supply with/without Consideration (Schedule I, II, III)" to "CGST Sec 7",
        "Composite vs Mixed Supply with Practical Illustrations" to "CGST Sec 8",
        "Levy, Reverse Charge Mechanism (RCM) & Composition Scheme" to "CGST Sec 9, 10"
      ),
      "GST: Time, Value & Input Tax Credit (ITC)" to listOf(
        "Time of Supply for Goods & Services" to "CGST Sec 12, 13",
        "Value of Supply & Discounts Treatment" to "CGST Sec 15",
        "Eligibility & Conditions for taking ITC, Blocked Credit u/s 17(5)" to "CGST Sec 16-17",
        "Registration under GST: Persons Liable, Procedure & Thresholds" to "CGST Sec 22-25",
        "Tax Invoice, Credit/Debit Notes, E-Way Bill & Returns (GSTR-1, 3B)" to "CGST Sec 31-39"
      )
    )

    p3Chapters.forEachIndexed { chIdx, (chName, topics) ->
      topics.forEachIndexed { tIdx, (title, ref) ->
        list.add(
          TopicItem(
            id = "p3_c${chIdx + 1}_t${tIdx + 1}",
            paperId = 3,
            chapterName = chName,
            topicTitle = title,
            icaiRef = ref,
            status = TopicStatus.PENDING
          )
        )
      }
    }

    // Paper 4: Cost & Management Accounting
    val p4Chapters = listOf(
      "Material, Labour & Overheads" to listOf(
        "EOQ, Re-order Levels, Inventory Turnover & Pricing Issues" to "Material",
        "Remuneration Systems (Halsey, Rowan) & Labour Turnover" to "Labour",
        "Allocation, Apportionment & Secondary Distribution of Overheads" to "Overheads",
        "Under/Over Absorption of Overheads & Treatment of Idle Capacity" to "Overheads"
      ),
      "Activity Based Costing & Cost Sheet" to listOf(
        "Cost Drivers, Cost Pools & ABC vs Traditional Absorption" to "ABC",
        "Preparation of Comprehensive Cost Sheet under CAS-1 to 24" to "Cost Sheet"
      ),
      "Methods of Costing (Process, Joint, Service)" to listOf(
        "Process Costing with Normal/Abnormal Loss & Equivalent Units" to "Process",
        "Joint Products & By-Products (NRV & Sales Value Method)" to "Joint Prod",
        "Service Costing: Transport, Hospital & Hotel Passenger-km" to "Service"
      ),
      "Standard Costing & Variance Analysis" to listOf(
        "Material Cost Variances (Price, Usage, Mix, Yield)" to "Material Var",
        "Labour Cost Variances (Rate, Efficiency, Idle Time, Mix)" to "Labour Var",
        "Variable & Fixed Overhead Variances" to "Overhead Var"
      ),
      "Marginal Costing & Budgetary Control" to listOf(
        "P/V Ratio, Break-Even Point (BEP) & Margin of Safety (MOS)" to "Marginal",
        "Key Factor Analysis, Make or Buy & Shutdown Decisions" to "Decision Making",
        "Flexible Budgets, Cash Budgets & Master Budget Preparation" to "Budgets"
      )
    )

    p4Chapters.forEachIndexed { chIdx, (chName, topics) ->
      topics.forEachIndexed { tIdx, (title, ref) ->
        list.add(
          TopicItem(
            id = "p4_c${chIdx + 1}_t${tIdx + 1}",
            paperId = 4,
            chapterName = chName,
            topicTitle = title,
            icaiRef = ref,
            status = TopicStatus.PENDING
          )
        )
      }
    }

    // Paper 5: Auditing and Ethics
    val p5Chapters = listOf(
      "Audit Fundamentals & Planning" to listOf(
        "SA 200: Overall Objectives of the Independent Auditor" to "SA 200",
        "SA 210: Agreeing the Terms of Audit Engagements" to "SA 210",
        "SA 220: Quality Management for an Audit of Financial Statements" to "SA 220",
        "SA 300: Planning an Audit of Financial Statements & Audit Strategy" to "SA 300"
      ),
      "Risk Assessment & Internal Control" to listOf(
        "SA 315: Identifying and Assessing the Risks of Material Misstatement" to "SA 315",
        "SA 320: Materiality in Planning and Performing an Audit" to "SA 320",
        "SA 330: The Auditor's Responses to Assessed Risks" to "SA 330",
        "Evaluation of Internal Control & Internal Financial Controls (IFC)" to "Sec 143(3)"
      ),
      "Audit Evidence & Procedures" to listOf(
        "SA 500: Audit Evidence - Sufficiency & Appropriateness" to "SA 500",
        "SA 501: Audit Evidence - Specific Considerations for Inventory" to "SA 501",
        "SA 505: External Confirmations (Positive vs Negative)" to "SA 505",
        "SA 520: Analytical Procedures & Ratio Comparisons" to "SA 520",
        "SA 530: Audit Sampling & Tolerable Error" to "SA 530"
      ),
      "Audit Documentation & Review" to listOf(
        "SA 230: Audit Documentation (Working Papers Ownership & Custody)" to "SA 230",
        "SA 560: Subsequent Events (Type 1 vs Type 2)" to "SA 560",
        "SA 570: Going Concern & Material Uncertainty" to "SA 570",
        "SA 580: Written Representations (Management Responsibility)" to "SA 580"
      ),
      "Audit Report, CARO 2020 & Professional Ethics" to listOf(
        "SA 700: Forming an Opinion & Reporting on Financial Statements" to "SA 700",
        "SA 705: Modifications to the Opinion (Qualified, Adverse, Disclaimer)" to "SA 705",
        "SA 706: Emphasis of Matter & Other Matter Paragraphs" to "SA 706",
        "Companies (Auditor's Report) Order 2020 (CARO 2020 Clauses)" to "CARO 2020",
        "Professional Ethics: Fundamental Principles & Threats to Independence" to "ICAI Ethics"
      )
    )

    p5Chapters.forEachIndexed { chIdx, (chName, topics) ->
      topics.forEachIndexed { tIdx, (title, ref) ->
        list.add(
          TopicItem(
            id = "p5_c${chIdx + 1}_t${tIdx + 1}",
            paperId = 5,
            chapterName = chName,
            topicTitle = title,
            icaiRef = ref,
            status = TopicStatus.PENDING
          )
        )
      }
    }

    // Paper 6: FM & Strategic Management
    val p6Chapters = listOf(
      "Financial Analysis & Ratio Analysis" to listOf(
        "Liquidity, Solvency & Turnover Ratios" to "Ratio Analysis",
        "Profitability Ratios & DuPont Analysis (3-point and 5-point)" to "DuPont",
        "Cash Flow Statement Analysis & Financial Health Diagnosis" to "FM Basics"
      ),
      "Cost of Capital & Capital Structure" to listOf(
        "Computation of Ke (Gordon, CAPM, Dividend Growth), Kd, Kp" to "Cost of Capital",
        "Weighted Average Cost of Capital (WACC) using Book vs Market Values" to "WACC",
        "Capital Structure Theories: Net Income, Net Operating Income, Traditional, MM" to "Capital Struct",
        "Operating, Financial & Combined Leverage (DOL, DFL, DCL)" to "Leverage"
      ),
      "Capital Budgeting & Working Capital Management" to listOf(
        "NPV, Internal Rate of Return (IRR) & Modified IRR" to "Cap Budgeting",
        "Profitability Index, Payback & Discounted Payback Period" to "Cap Budgeting",
        "Operating Cycle Estimation & Working Capital Financing Methods" to "Working Capital",
        "Receivables Management & Cash Budget Forecasting" to "Working Capital"
      ),
      "Strategic Analysis: External & Internal Environment (SM)" to listOf(
        "PESTEL Analysis & Porter's Five Forces Industry Competitiveness" to "SM Framework",
        "SWOT Analysis, Value Chain Analysis & Core Competencies" to "SM Internal",
        "Portfolio Analysis: BCG Growth-Share Matrix, GE Model, Ansoff Matrix" to "Portfolio Analysis"
      ),
      "Strategic Choices & Implementation (SM)" to listOf(
        "Michael Porter's Generic Strategies: Cost Leadership, Differentiation, Focus" to "Generic Strategy",
        "Corporate Growth Strategies: Diversification, Mergers & Joint Ventures" to "Corp Strategy",
        "Strategy Implementation: Structure, Culture, McKinsey 7S Framework" to "Implementation",
        "Strategic Control, Digital Transformation & Balanced Scorecard" to "Strategic Control"
      )
    )

    p6Chapters.forEachIndexed { chIdx, (chName, topics) ->
      topics.forEachIndexed { tIdx, (title, ref) ->
        list.add(
          TopicItem(
            id = "p6_c${chIdx + 1}_t${tIdx + 1}",
            paperId = 6,
            chapterName = chName,
            topicTitle = title,
            icaiRef = ref,
            status = TopicStatus.PENDING
          )
        )
      }
    }

    return list
  }
}
