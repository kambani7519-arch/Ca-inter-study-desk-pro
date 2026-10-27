package com.example.data.questionbank

import com.example.data.model.CaseScenarioMcq
import com.example.data.model.DescriptiveQuestion
import com.example.data.model.GeneratedTestPaper
import com.example.data.model.RapidFireCard
import java.util.UUID

object CuratedQuestionBank {

  fun generateTest(paperId: Int, chapterOrStandard: String, marks: Int): GeneratedTestPaper {
    val mcqPercentage = 0.30f
    val mcqMarks = (marks * mcqPercentage).toInt().coerceAtLeast(6)
    val descriptiveMarks = marks - mcqMarks

    // Approximate count: each MCQ is 2 marks
    val mcqCount = (mcqMarks / 2).coerceAtLeast(3)
    val descCount = when {
      marks <= 20 -> 2 // e.g. 7 + 7 marks
      marks <= 30 -> 3 // e.g. 7 + 7 + 7 marks
      marks <= 50 -> 4 // e.g. 10 + 9 + 8 + 8
      else -> 6 // 100 marks full paper
    }

    val paperMcqs = getSampleMcqs(paperId).shuffled().take(mcqCount)
    val paperDescriptive = getSampleDescriptive(paperId).shuffled().take(descCount)

    val paperName = when (paperId) {
      1 -> "Paper 1: Advanced Accounting"
      2 -> "Paper 2: Corporate & Other Laws"
      3 -> "Paper 3: Taxation (DT & GST)"
      4 -> "Paper 4: Cost & Management Accounting"
      5 -> "Paper 5: Auditing & Ethics"
      else -> "Paper 6: FM & Strategic Management"
    }

    val title = if (marks == 100) {
      "ICAI Jan 2027 Mock Exam (100 Marks Full Syllabus) - $paperName"
    } else {
      "ICAI Practice Test ($marks Marks) - $chapterOrStandard"
    }

    return GeneratedTestPaper(
      testId = "test_" + UUID.randomUUID().toString().take(8),
      title = title,
      paperId = paperId,
      chapterOrStandard = chapterOrStandard,
      totalMarks = marks,
      mcqMarks = mcqMarks,
      descriptiveMarks = descriptiveMarks,
      mcqs = paperMcqs,
      descriptiveQuestions = paperDescriptive
    )
  }

  fun getSampleMcqs(paperId: Int): List<CaseScenarioMcq> {
    return when (paperId) {
      1 -> listOf(
        CaseScenarioMcq(
          id = "mcq_p1_1",
          scenarioText = "Zenith Ltd. acquired a heavy earthmoving machine on 1st April 2024 for ₹50,00,000. It incurred ₹2,00,000 as installation expenses and ₹3,00,000 on site preparation. The company also spent ₹50,000 on opening party of the new plant. Estimated useful life is 10 years with ₹5,00,000 salvage value.",
          question = "As per AS 10 (Property, Plant and Equipment), what is the initial carrying amount at which the machinery should be recognized in the books?",
          options = listOf("₹55,50,000", "₹55,00,000", "₹50,00,000", "₹52,00,000"),
          correctOptionIndex = 1,
          icaiCitation = "AS 10 (Revised) Property, Plant and Equipment, Para 16-19",
          detailedExplanation = "Under AS 10, cost includes purchase price, import duties, and directly attributable costs of bringing asset to working condition (installation ₹2L + site preparation ₹3L = ₹55,00,000). Costs of opening new facility or promotional events (₹50,000) must be expensed in P&L."
        ),
        CaseScenarioMcq(
          id = "mcq_p1_2",
          scenarioText = "A holding company H Ltd. holds 75% equity in S Ltd. During the financial year, S Ltd. sold inventory costing ₹4,00,000 to H Ltd. for ₹5,00,000. At the year-end, 40% of this inventory remains unsold in the godown of H Ltd.",
          question = "As per AS 21 (Consolidated Financial Statements), what amount of unrealised profit should be eliminated from the consolidated financial statements?",
          options = listOf("₹1,00,000", "₹40,000", "₹30,000", "₹75,000"),
          correctOptionIndex = 1,
          icaiCitation = "AS 21 Consolidated Financial Statements, Para 16-17",
          detailedExplanation = "Total inter-company profit is ₹1,00,000 (₹5,00,000 - ₹4,00,000). Since 40% remains in stock, unrealised profit is 40% of ₹1,00,000 = ₹40,000. Under AS 21, the entire unrealised intra-group profit (₹40,000) must be eliminated."
        ),
        CaseScenarioMcq(
          id = "mcq_p1_3",
          scenarioText = "Apex Ltd. has entered into a non-cancellable lease for a generator set for 5 years. The fair value of the asset at lease inception is ₹10,00,000. Present value of Minimum Lease Payments (MLP) discounted at interest rate implicit in the lease is ₹9,20,000.",
          question = "Under AS 19 (Leases), how should this lease be classified by the lessee?",
          options = listOf("Operating Lease", "Finance Lease", "Sale and Leaseback transaction", "Short-term lease"),
          correctOptionIndex = 1,
          icaiCitation = "AS 19 Leases, Para 8(c)",
          detailedExplanation = "As per AS 19, a lease is classified as a finance lease if at the inception of lease, the present value of minimum lease payments substantially equals (typically ≥ 90%) the fair value of the leased asset. Here PV is 92% of fair value."
        ),
        CaseScenarioMcq(
          id = "mcq_p1_4",
          scenarioText = "Bright Ltd. decides to buy back 2,00,000 equity shares of face value ₹10 each at a premium of ₹25 per share out of its Free Reserves. The shares were fully paid up.",
          question = "What amount must be transferred to the Capital Redemption Reserve (CRR) Account as per Section 69 of Companies Act, 2013?",
          options = listOf("₹70,00,000", "₹50,00,000", "₹20,00,000", "₹35,00,000"),
          correctOptionIndex = 2,
          icaiCitation = "Companies Act 2013, Section 69(1)",
          detailedExplanation = "Where buyback of shares is made out of free reserves or securities premium account, a sum equal to the nominal (face) value of the shares so purchased must be transferred to CRR. Nominal value = 2,00,000 × ₹10 = ₹20,00,000."
        )
      )
      2 -> listOf(
        CaseScenarioMcq(
          id = "mcq_p2_1",
          scenarioText = "PQR Ltd., a public company having 1,500 members, scheduled its Annual General Meeting on 28th September at 11:00 AM. By 11:30 AM, only 12 members were personally present. The Articles of Association are silent on quorum.",
          question = "What is the consequence under Section 103 of the Companies Act, 2013?",
          options = listOf(
            "The meeting can proceed because 10 members constitute quorum.",
            "The meeting shall stand adjourned to the same day in next week at the same time and place.",
            "The meeting stands dissolved automatically.",
            "The Chairman can declare quorum by counting proxy holders."
          ),
          correctOptionIndex = 1,
          icaiCitation = "Companies Act 2013, Section 103(1)(a) & 103(2)",
          detailedExplanation = "Under Section 103, for a public company with members exceeding 1,000 but up to 5,000, 15 members personally present are required for quorum. If quorum is not present within 30 minutes, meeting stands adjourned to the same day in next week."
        ),
        CaseScenarioMcq(
          id = "mcq_p2_2",
          scenarioText = "A director of Alpha Ltd. accepted a deposit of ₹10,00,000 from an outsider who was unaware that under the company's Articles, directors can only borrow up to ₹5,00,000 without shareholders' approval in general meeting.",
          question = "Can the lender enforce the loan against the company relying on the Doctrine of Indoor Management?",
          options = listOf(
            "Yes, because internal compliance is the company's responsibility.",
            "No, because the outsider has constructive notice of public documents including borrowing limits in MOA/AOA.",
            "Yes, if the director acted in good faith.",
            "No, unless ratified by NCLT."
          ),
          correctOptionIndex = 1,
          icaiCitation = "Royal British Bank v. Turquand; Section 399 Constructive Notice",
          detailedExplanation = "The Doctrine of Indoor Management cannot override the Doctrine of Constructive Notice where the restriction is contained in the public documents (MOA/AOA). A person dealing with a company is presumed to know its registered constitutional limits."
        ),
        CaseScenarioMcq(
          id = "mcq_p2_3",
          scenarioText = "Kavita LLP was incorporated with 4 partners. On 10th January, two partners resigned and one partner expired. The remaining sole partner continues to carry on business beyond six months.",
          question = "Under Section 6 of the LLP Act, 2008, what is the legal liability of the sole remaining partner?",
          options = listOf(
            "The partner is personally liable for the obligations incurred after six months.",
            "The LLP continues with limited liability indefinitely.",
            "The Registrar automatically strikes off the LLP within 30 days.",
            "The partner is exempt if notice was given to ROC."
          ),
          correctOptionIndex = 0,
          icaiCitation = "Limited Liability Partnership Act 2008, Section 6(2)",
          detailedExplanation = "Under Section 6(2) of LLP Act 2008, if at any time the number of partners is reduced below two and the LLP carries on business for more than six months, the person who continues is personally liable for all obligations of the LLP incurred during that period."
        )
      )
      3 -> listOf(
        CaseScenarioMcq(
          id = "mcq_p3_1",
          scenarioText = "Mr. Raghav, aged 45, an Indian citizen, left India for employment in Canada on 15th August 2024. He had never gone abroad before. His Indian income for FY 2024-25 is ₹12,00,000.",
          question = "What is the residential status of Mr. Raghav for the Assessment Year 2025-26 under Section 6(1) of the Income-tax Act, 1961?",
          options = listOf("Resident and Ordinarily Resident", "Resident but Not Ordinarily Resident", "Non-Resident", "Deemed Resident u/s 6(1A)"),
          correctOptionIndex = 2,
          icaiCitation = "Income-tax Act 1961, Section 6(1)(c) Explanation 1(a)",
          detailedExplanation = "An Indian citizen leaving India for employment requires 182 days stay in the previous year to be resident. Raghav stayed from 1st April to 15th August = 30 + 31 + 30 + 31 + 15 = 137 days (< 182 days). Hence he is Non-Resident."
        ),
        CaseScenarioMcq(
          id = "mcq_p3_2",
          scenarioText = "A registered supplier under GST issued a tax invoice of ₹1,00,000 + 18% GST to M/s Bharat Ltd. Bharat Ltd. made full payment including tax to the supplier, but the supplier failed to file GSTR-1 and pay tax to the Government.",
          question = "Can Bharat Ltd. claim and retain Input Tax Credit (ITC) under Section 16(2)(c) of the CGST Act, 2017?",
          options = listOf(
            "Yes, because buyer possesses invoice and has paid the supplier.",
            "No, payment of tax to Government by supplier is a mandatory statutory condition.",
            "Yes, subject to a 10% penalty on the supplier.",
            "Yes, if buyer files self-assessed DRC-03."
          ),
          correctOptionIndex = 1,
          icaiCitation = "CGST Act 2017, Section 16(2)(c)",
          detailedExplanation = "Under Section 16(2)(c) of the CGST Act, 2017, one of the four essential conditions to avail ITC is that the tax charged in respect of such supply has been actually paid to the Government, either in cash or through utilization of input tax credit."
        )
      )
      4 -> listOf(
        CaseScenarioMcq(
          id = "mcq_p4_1",
          scenarioText = "Standard material required for 1 unit of product X is 5 kg at ₹20 per kg. Actual production was 1,000 units for which 5,400 kg of material was consumed at ₹19 per kg.",
          question = "What is the Material Usage Variance (MUV)?",
          options = listOf("₹8,000 (Adverse)", "₹8,000 (Favourable)", "₹5,400 (Favourable)", "₹2,600 (Adverse)"),
          correctOptionIndex = 0,
          icaiCitation = "Standard Costing - Material Variances",
          detailedExplanation = "MUV = Standard Price × (Standard Quantity for actual output - Actual Quantity) = ₹20 × (5,000 kg - 5,400 kg) = ₹20 × (-400) = ₹8,000 (Adverse)."
        ),
        CaseScenarioMcq(
          id = "mcq_p4_2",
          scenarioText = "A company has a P/V ratio of 40% and Margin of Safety (MOS) of 30%. Total sales during the year were ₹50,00,000.",
          question = "What is the net profit earned by the company?",
          options = listOf("₹6,00,000", "₹12,00,000", "₹8,00,000", "₹15,00,000"),
          correctOptionIndex = 0,
          icaiCitation = "Marginal Costing - Profit Volume Analysis",
          detailedExplanation = "Profit = Margin of Safety Sales × P/V Ratio = (30% of ₹50,00,000) × 40% = ₹15,00,000 × 0.40 = ₹6,00,000."
        )
      )
      5 -> listOf(
        CaseScenarioMcq(
          id = "mcq_p5_1",
          scenarioText = "During the audit of Star Ltd., the statutory auditor requested direct positive external confirmation from debtors. Management refused to allow the auditor to send the requests stating pending commercial dispute.",
          question = "As per SA 505 (External Confirmations), what is the auditor's immediate required procedure?",
          options = listOf(
            "Inquire as to management's reasons for the refusal, and seek audit evidence as to their validity and reasonableness.",
            "Issue an immediate disclaimer of opinion without any further inquiry.",
            "Withdraw from the audit engagement immediately and inform ICAI.",
            "Accept management's explanation since debtor confirmations are optional."
          ),
          correctOptionIndex = 0,
          icaiCitation = "SA 505 External Confirmations, Paragraph 8",
          detailedExplanation = "As per SA 505, if management refuses to allow the auditor to send confirmation requests, the auditor shall: (a) Inquire regarding management's reasons, (b) Evaluate implications on risk of fraud and other procedures, and (c) Perform alternative audit procedures."
        ),
        CaseScenarioMcq(
          id = "mcq_p5_2",
          scenarioText = "An auditor encounters physical inventory verification at year-end that was impossible to attend due to natural calamity. The auditor obtained sufficient alternative evidence via subsequent sales and warehouse records.",
          question = "Under SA 700 / SA 705, which type of audit opinion should the auditor issue?",
          options = listOf(
            "Unmodified Opinion with an Emphasis of Matter paragraph if material",
            "Qualified Opinion due to scope limitation",
            "Adverse Opinion",
            "Disclaimer of Opinion"
          ),
          correctOptionIndex = 0,
          icaiCitation = "SA 501 Para 7 & SA 700 / SA 706",
          detailedExplanation = "If attendance at physical inventory counting is impracticable, the auditor shall perform alternative audit procedures to obtain sufficient appropriate audit evidence. If sufficient evidence is obtained, an unmodified opinion is appropriate."
        )
      )
      else -> listOf(
        CaseScenarioMcq(
          id = "mcq_p6_1",
          scenarioText = "A firm has sales of ₹20,00,000, variable costs of ₹12,00,000, fixed costs of ₹4,00,000, and interest expenses of ₹1,00,000. Corporate tax rate is 30%.",
          question = "What is the Combined Leverage (DCL) of the firm?",
          options = listOf("2.67", "2.00", "1.33", "3.00"),
          correctOptionIndex = 0,
          icaiCitation = "Financial Management - Leverages",
          detailedExplanation = "Contribution = Sales - Variable Cost = ₹20L - ₹12L = ₹8L. EBIT = Contribution - Fixed Cost = ₹8L - ₹4L = ₹4L. EBT = EBIT - Interest = ₹4L - ₹1L = ₹3L. Degree of Combined Leverage (DCL) = Contribution / EBT = 8,00,000 / 3,00,000 = 2.67."
        ),
        CaseScenarioMcq(
          id = "mcq_p6_2",
          scenarioText = "Under Michael Porter's Generic Strategies, a luxury sports car manufacturer targets a narrow high-net-worth customer segment with bespoke handcrafted vehicles.",
          question = "Which generic competitive strategy is being pursued?",
          options = listOf("Cost Leadership", "Differentiation Focus", "Cost Focus", "Broad Differentiation"),
          correctOptionIndex = 1,
          icaiCitation = "Strategic Management - Michael Porter Generic Strategies",
          detailedExplanation = "Differentiation Focus occurs when a business seeks to differentiate its product or service within a narrow, focused niche target market rather than appealing across the entire industry."
        )
      )
    }
  }

  fun getSampleDescriptive(paperId: Int): List<DescriptiveQuestion> {
    return when (paperId) {
      1 -> listOf(
        DescriptiveQuestion(
          id = "desc_p1_1",
          questionNumber = "Q1 (Practical)",
          questionText = "Solaris Ltd. purchased machinery on 01-04-2021 for ₹80,00,000 (useful life 10 years, residual value nil, straight line method). On 01-04-2023, the asset was revalued upwards to ₹90,00,000. On 01-04-2024, due to technological obsolescence, the recoverable amount of the machinery was assessed at ₹45,00,000. Advise on accounting treatment under AS 10 & AS 28.",
          marks = 10,
          icaiReference = "AS 10 (PPE) & AS 28 (Impairment of Assets)",
          rubricSteps = listOf(
            "Depreciation calculation up to revaluation (2 years: ₹16,00,000; Book Value = ₹64,00,000)",
            "Revaluation Surplus credited to Reserves: ₹26,00,000",
            "Depreciation on revalued amount for FY 23-24 (₹90,00,000 / 8 = ₹11,25,000)",
            "Carrying amount before impairment on 01-04-2024 = ₹78,75,000",
            "Impairment loss = ₹78,75,000 - ₹45,00,000 = ₹33,75,000 (Adjust first against revaluation reserve)",
            "Working Notes neatly drafted with ledger balances"
          ),
          officialModelAnswer = "1. Relevant Standards: AS 10 (Revised) & AS 28.\n2. Book Value at 01-04-2023 = ₹80L - (₹8L * 2) = ₹64,00,000.\n3. Revaluation upward = ₹90L - ₹64L = ₹26,00,000 credited to Revaluation Reserve.\n4. Dep. FY 23-24 = ₹90L / 8 yrs = ₹11,25,000. Carrying Amount = ₹78,75,000.\n5. Recoverable amount = ₹45,00,000. Impairment loss = ₹33,75,000. Out of this, ₹26,00,000 is debited to Revaluation Surplus and balance ₹7,75,000 is recognized in P&L.",
          keyPhrases = listOf("AS 10", "AS 28", "Revaluation Reserve", "Recoverable Amount", "Carrying Amount", "Straight Line Method")
        ),
        DescriptiveQuestion(
          id = "desc_p1_2",
          questionNumber = "Q2 (Consolidation)",
          questionText = "Holdco Ltd. acquired 80% shares of Subco Ltd. on 01-10-2023. Financial year ends on 31-03-2024. Subco Ltd. had General Reserve of ₹2,00,000 and P&L Balance of ₹1,00,000 on 01-04-2023. Profit earned during 2023-24 was ₹3,60,000 evenly. Calculate Pre-acquisition and Post-acquisition profits.",
          marks = 8,
          icaiReference = "AS 21 Consolidated Financial Statements",
          rubricSteps = listOf(
            "Time apportionment of current year profit (6 months pre, 6 months post)",
            "Total pre-acquisition reserves calculation",
            "Cost of Control / Minority Interest allocation",
            "Presentation of working note"
          ),
          officialModelAnswer = "1. Pre-acquisition profit = Opening reserve ₹2L + Opening P&L ₹1L + (6/12 * ₹3.6L) = ₹4,80,000. Holdco share (80%) = ₹3,84,000.\n2. Post-acquisition profit = Remaining 6 months profit = ₹1,80,000. Holdco share (80%) = ₹1,44,000 credited to Consolidated P&L.\n3. Minority Interest gets 20% of net worth.",
          keyPhrases = listOf("AS 21", "Pre-acquisition", "Post-acquisition", "Cost of Control", "Minority Interest")
        )
      )
      2 -> listOf(
        DescriptiveQuestion(
          id = "desc_p2_1",
          questionNumber = "Q1 (Corporate Law)",
          questionText = "The Board of Directors of Neptune Ltd. proposed a final dividend of 15% on equity share capital of ₹10 Crores for FY 2023-24. The company suffered net losses during current year and proposes to declare dividend out of accumulated profits of previous years. Explain the conditions specified in Companies (Declaration and Payment of Dividend) Rules, 2014.",
          marks = 7,
          icaiReference = "Section 123(1) read with Rule 3 of Dividend Rules 2014",
          rubricSteps = listOf(
            "State Rule 3(1): Rate of dividend shall not exceed average of preceding 3 years",
            "State Rule 3(2): Total withdrawal shall not exceed 10% of (Paid up capital + Free Reserves)",
            "State Rule 3(3): Amount drawn first set off against current year loss",
            "State Rule 3(4): Balance of reserves after withdrawal shall not fall below 15% of paid-up capital",
            "Conclusion on legality of 15% dividend declaration"
          ),
          officialModelAnswer = "1. Provision: Under Section 123(1) and Rule 3 of Companies (Declaration and Payment of Dividend) Rules, 2014, in case of inadequacy or absence of profits, dividend can only be declared subject to: (a) Rate <= Avg rate of preceding 3 years, (b) Withdrawal <= 10% of Paid-up Capital + Free Reserves, (c) Set-off of current losses first, (d) Residual reserves >= 15% of paid-up share capital.\n2. Application & Conclusion: Neptune Ltd. must satisfy all 4 cumulative conditions before declaring dividend.",
          keyPhrases = listOf("Section 123", "Rule 3", "Accumulated Profits", "Paid-up Share Capital", "Free Reserves", "Preceding three years average")
        ),
        DescriptiveQuestion(
          id = "desc_p2_2",
          questionNumber = "Q2 (Company Law)",
          questionText = "Explain the statutory provisions regarding appointment and qualification of Corporate Social Responsibility (CSR) Committee under Section 135 of the Companies Act, 2013.",
          marks = 6,
          icaiReference = "Section 135 Companies Act 2013 & CSR Rules",
          rubricSteps = listOf(
            "Eligibility criteria: Net worth >= ₹500 Cr OR Turnover >= ₹1000 Cr OR Net Profit >= ₹5 Cr",
            "Composition: 3 or more directors with at least 1 Independent Director",
            "Minimum spending: 2% of average net profits of preceding 3 financial years",
            "Treatment of unspent CSR account"
          ),
          officialModelAnswer = "1. Statutory Thresholds: Sec 135(1) triggers if Net Worth >= 500 Cr, Turnover >= 1000 Cr, or Net Profit >= 5 Cr during immediately preceding financial year.\n2. Composition: Min 3 directors (at least 1 independent).\n3. Expenditure: Min 2% of average net profits u/s 198 of 3 preceding years.\n4. Ongoing projects transfer to Unspent CSR A/c within 30 days.",
          keyPhrases = listOf("Section 135", "Net Worth 500 Crore", "Turnover 1000 Crore", "Net Profit 5 Crore", "2 percent", "Unspent CSR Account")
        )
      )
      3 -> listOf(
        DescriptiveQuestion(
          id = "desc_p3_1",
          questionNumber = "Q1 (Income Tax)",
          questionText = "State the conditions under which a salaried employee can claim exemption for House Rent Allowance (HRA) under Section 10(13A) read with Rule 2A of the Income-tax Rules, 1962.",
          marks = 6,
          icaiReference = "Section 10(13A) & Rule 2A",
          rubricSteps = listOf(
            "State the three statutory limits: Actual HRA received",
            "Rent paid minus 10% of Salary",
            "50% of Salary for Metro (Mumbai, Delhi, Kolkata, Chennai) or 40% for Non-Metro",
            "Definition of Salary (Basic + DA forming part + Commission on turnover)"
          ),
          officialModelAnswer = "Under Section 10(13A) and Rule 2A, the least of the following is exempt:\n(i) Actual HRA received;\n(ii) Rent paid in excess of 10% of salary;\n(iii) 50% of salary (for Mumbai, Delhi, Kolkata, Chennai) or 40% of salary (other cities).\nSalary means Basic + DA (terms of employment) + percentage turnover commission.",
          keyPhrases = listOf("Section 10(13A)", "Rule 2A", "Rent paid minus 10%", "50 percent of salary", "Basic plus DA")
        ),
        DescriptiveQuestion(
          id = "desc_p3_2",
          questionNumber = "Q2 (GST)",
          questionText = "What constitutes 'Blocked Credit' under Section 17(5) of the CGST Act, 2017? Discuss with special reference to motor vehicles and food & beverages.",
          marks = 7,
          icaiReference = "Section 17(5) of CGST Act, 2017",
          rubricSteps = listOf(
            "Motor vehicles with seating capacity <= 13 persons blocked, with specific exceptions",
            "Exceptions: Further supply of vehicles, passenger transport, driving training",
            "Food, beverages, outdoor catering, beauty treatment blocked unless used for outward taxable supply of same category",
            "Employee welfare statutory obligation exception"
          ),
          officialModelAnswer = "Section 17(5) blocks ITC on:\n1. Motor vehicles for transportation of persons having approved seating capacity of <= 13 persons (including driver), unless used for further supply, passenger transport or driving training.\n2. Food, beverages, outdoor catering, beauty treatment, health services, unless used to make an outward taxable supply of same category or obligatory by law for employer to provide to employees.",
          keyPhrases = listOf("Section 17(5)", "Blocked Credit", "Motor Vehicles", "Seating capacity 13 persons", "Food and beverages", "Obligatory by law")
        )
      )
      4 -> listOf(
        DescriptiveQuestion(
          id = "desc_p4_1",
          questionNumber = "Q1 (Marginal Costing)",
          questionText = "A manufacturing company produces two products A and B. Selling price is ₹100 and ₹120. Variable cost is ₹60 and ₹84. Machine hours required are 2 hours for A and 3 hours for B. Total available machine hours are 12,000 hours. Market demand is 4,000 units for A and 3,000 units for B. Determine the optimal product mix.",
          marks = 8,
          icaiReference = "Marginal Costing - Key Factor / Limiting Factor Analysis",
          rubricSteps = listOf(
            "Contribution per unit: A = ₹40, B = ₹36",
            "Contribution per machine hour (Key Factor): A = ₹40 / 2 = ₹20/hr, B = ₹36 / 3 = ₹12/hr",
            "Ranking: Rank 1 to Product A, Rank 2 to Product B",
            "Allocation: Produce max A (4,000 units * 2 = 8,000 hrs); Remaining 4,000 hrs for B (4,000 / 3 = 1,333 units)",
            "Total Contribution computation"
          ),
          officialModelAnswer = "Key Factor is Machine Hours.\nContribution/unit: A = ₹40, B = ₹36.\nContribution per machine hr: A = ₹40/2 = ₹20/hr; B = ₹36/3 = ₹12/hr.\nRanking: Product A is Rank I, Product B is Rank II.\nOptimal Production: Product A = 4,000 units (utilizing 8,000 hrs); Product B = 1,333 units (utilizing balance 4,000 hrs). Total Contribution = ₹2,07,988.",
          keyPhrases = listOf("Key Factor", "Contribution per machine hour", "Ranking", "Optimal Product Mix", "Limiting Factor")
        )
      )
      5 -> listOf(
        DescriptiveQuestion(
          id = "desc_p5_1",
          questionNumber = "Q1 (Standards on Auditing)",
          questionText = "State the essential requirements of SA 230 'Audit Documentation' regarding assembly of the final audit file and the retention period of audit working papers.",
          marks = 6,
          icaiReference = "SA 230 Audit Documentation & SQC 1",
          rubricSteps = listOf(
            "Assembly deadline: Within 60 days after the date of the auditor's report (SQC 1)",
            "Administrative process: No new audit procedures or new conclusions during assembly",
            "Retention period: Minimum 7 years from the date of the auditor's report",
            "Ownership: Working papers are the property of the auditor"
          ),
          officialModelAnswer = "Under SA 230 and SQC 1:\n1. Assembly: The auditor shall assemble the audit documentation in an audit file on a timely basis, not exceeding 60 days after the date of the auditor's report.\n2. Modifications: The completion of assembly is an administrative process that does not involve performing new procedures.\n3. Retention: The retention period is ordinarily not shorter than 7 years from the date of the auditor's report.\n4. Ownership: Working papers are the property of the auditor.",
          keyPhrases = listOf("SA 230", "SQC 1", "60 days", "Retention period not shorter than 7 years", "Property of the auditor", "Assembly of final audit file")
        ),
        DescriptiveQuestion(
          id = "desc_p5_2",
          questionNumber = "Q2 (Audit Report)",
          questionText = "Differentiate between an Emphasis of Matter (EOM) paragraph and Key Audit Matters (KAM) in the Independent Auditor's Report as per SA 706 and SA 701.",
          marks = 6,
          icaiReference = "SA 706 (Revised) & SA 701",
          rubricSteps = listOf(
            "Definition of EOM: Refers to matter appropriately presented/disclosed in FS fundamental to users' understanding",
            "Definition of KAM: Matters of most significance in the audit of financial statements of listed entities",
            "EOM does not substitute for modified opinion or KAM",
            "KAM is mandatory for listed entities u/s 143 / SA 701"
          ),
          officialModelAnswer = "1. SA 706 (Emphasis of Matter): Refers to a matter appropriately presented or disclosed in the financial statements that, in the auditor's judgment, is fundamental to users' understanding. It does not modify opinion.\n2. SA 701 (Key Audit Matters): Those matters that, in the auditor's professional judgment, were of most significance in the audit of the current period, selected from matters communicated with TCWG.\n3. Key Distinction: KAM is mandatory for listed entities and represents auditor attention, whereas EOM draws attention to specific fundamental disclosures.",
          keyPhrases = listOf("SA 706", "SA 701", "Emphasis of Matter", "Key Audit Matters", "Fundamental to users understanding", "Those Charged with Governance")
        )
      )
      else -> listOf(
        DescriptiveQuestion(
          id = "desc_p6_1",
          questionNumber = "Q1 (Financial Management)",
          questionText = "Apex Ltd. has an equity share capital of ₹50,00,000 (face value ₹10). Current market price is ₹80. The company recently paid a dividend of ₹6 per share and dividend is expected to grow at 8% per annum indefinitely. Calculate Cost of Equity (Ke) using Gordon's Dividend Growth Model.",
          marks = 6,
          icaiReference = "Financial Management - Cost of Capital",
          rubricSteps = listOf(
            "Identify D0 = ₹6.00, g = 8%, P0 = ₹80",
            "Calculate expected dividend D1 = D0 * (1 + g) = ₹6 * 1.08 = ₹6.48",
            "Apply formula Ke = (D1 / P0) + g",
            "Ke = (6.48 / 80) + 0.08 = 0.081 + 0.08 = 16.1%",
            "State interpretation"
          ),
          officialModelAnswer = "Formula: Ke = (D1 / P0) + g\nWhere D0 = ₹6, g = 0.08, P0 = ₹80.\nD1 = D0 * (1 + 0.08) = ₹6.48.\nKe = (₹6.48 / ₹80) + 0.08 = 0.081 + 0.08 = 16.10%.\nThe Cost of Equity for Apex Ltd. is 16.10%.",
          keyPhrases = listOf("Cost of Equity", "Dividend Growth Model", "D1 divided by P0 plus g", "16.10 percent", "Expected Dividend")
        ),
        DescriptiveQuestion(
          id = "desc_p6_2",
          questionNumber = "Q2 (Strategic Management)",
          questionText = "Explain the BCG (Boston Consulting Group) Growth-Share Matrix. Describe the strategy recommendations for 'Stars', 'Cash Cows', 'Question Marks', and 'Dogs'.",
          marks = 7,
          icaiReference = "Strategic Management - Portfolio Analysis",
          rubricSteps = listOf(
            "Matrix axes: Market Growth Rate (Vertical) vs Relative Market Share (Horizontal)",
            "Stars (High Growth, High Share): Build or Hold strategy, heavy investment",
            "Cash Cows (Low Growth, High Share): Harvest strategy, generates cash",
            "Question Marks (High Growth, Low Share): Build or Divest strategy",
            "Dogs (Low Growth, Low Share): Divest or Liquidate strategy",
            "Neat 2x2 matrix representation"
          ),
          officialModelAnswer = "BCG Matrix classifies business units based on Market Growth Rate and Relative Market Share into 4 quadrants:\n1. Stars (High Growth, High Share): Require heavy investment to maintain dominance. Strategy: Build.\n2. Cash Cows (Low Growth, High Share): Foundation of company, generate surplus cash with low reinvestment need. Strategy: Hold/Harvest.\n3. Question Marks (High Growth, Low Share): Require cash infusion to become stars. Strategy: Build or Divest.\n4. Dogs (Low Growth, Low Share): Low profits or cash drains. Strategy: Divest or Liquidate.",
          keyPhrases = listOf("BCG Matrix", "Market Growth Rate", "Relative Market Share", "Stars", "Cash Cows", "Question Marks", "Dogs", "Harvest", "Divest")
        )
      )
    }
  }

  fun getRapidFireCards(): List<RapidFireCard> {
    return listOf(
      RapidFireCard(
        id = "rf_1",
        paperId = 1,
        subjectName = "Advanced Accounting",
        question = "Which Accounting Standard governs Property, Plant and Equipment, and what is its treatment of administrative opening expenses?",
        answer = "AS 10 Revised. Administrative opening expenses cannot be capitalized and must be charged directly to Profit and Loss.",
        keyProvision = "AS 10 (Revised) Para 16-19"
      ),
      RapidFireCard(
        id = "rf_2",
        paperId = 1,
        subjectName = "Advanced Accounting",
        question = "What is the maximum limit on buyback of equity shares in any financial year under Section 68 of the Companies Act?",
        answer = "25% of the total paid-up equity capital in that financial year, and overall buyback cannot exceed 25% of paid-up capital + free reserves.",
        keyProvision = "Section 68(2) Companies Act, 2013"
      ),
      RapidFireCard(
        id = "rf_3",
        paperId = 2,
        subjectName = "Corporate & Other Laws",
        question = "What is the statutory quorum for an AGM in a public company having 2,500 members?",
        answer = "15 members personally present under Section 103(1)(a)(ii).",
        keyProvision = "Section 103 Companies Act, 2013"
      ),
      RapidFireCard(
        id = "rf_4",
        paperId = 2,
        subjectName = "Corporate & Other Laws",
        question = "Under Section 135, what is the mandatory minimum CSR expenditure for an eligible company?",
        answer = "At least 2% of the average net profits of the company made during the three immediately preceding financial years.",
        keyProvision = "Section 135(5) Companies Act, 2013"
      ),
      RapidFireCard(
        id = "rf_5",
        paperId = 3,
        subjectName = "Taxation",
        question = "Under Section 16(2) of CGST Act, what are the four mandatory conditions for availing Input Tax Credit?",
        answer = "1. Possession of tax invoice, 2. Receipt of goods or services, 3. Tax paid to the government by supplier, 4. Furnishing of return u/s 39.",
        keyProvision = "CGST Act, 2017 Section 16(2)"
      ),
      RapidFireCard(
        id = "rf_6",
        paperId = 4,
        subjectName = "Cost & Management Accounting",
        question = "How is Margin of Safety (MOS) calculated and what does it indicate?",
        answer = "MOS = Actual Sales minus Break-Even Sales, or Profit divided by P/V Ratio. It indicates the buffer volume before the firm incurs a loss.",
        keyProvision = "Marginal Costing - CVP Analysis"
      ),
      RapidFireCard(
        id = "rf_7",
        paperId = 5,
        subjectName = "Auditing & Ethics",
        question = "What is the retention period of audit working papers under SA 230 and SQC 1?",
        answer = "Not shorter than 7 years from the date of the auditor's report (or group auditor's report).",
        keyProvision = "SA 230 & SQC 1"
      ),
      RapidFireCard(
        id = "rf_8",
        paperId = 6,
        subjectName = "FM & Strategic Management",
        question = "What does Degree of Operating Leverage (DOL) measure and what is its formula?",
        answer = "DOL measures the sensitivity of Operating Profit (EBIT) to changes in Sales. Formula: Contribution divided by EBIT.",
        keyProvision = "FM Leverage Analysis"
      )
    )
  }
}
